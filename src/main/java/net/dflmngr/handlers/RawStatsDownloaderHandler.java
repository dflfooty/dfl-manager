package net.dflmngr.handlers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
//import org.openqa.selenium.htmlunit.HtmlUnitDriver;
//import org.openqa.selenium.phantomjs.PhantomJSDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

//import com.gargoylesoftware.htmlunit.BrowserVersion;
//import com.gargoylesoftware.htmlunit.WebClient;

//import io.github.bonigarcia.wdm.PhantomJsDriverManager;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.RawPlayerStats;
//import net.dflmngr.model.entity.keys.ProcessPK;
import net.dflmngr.model.entity.Process;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.ProcessService;
import net.dflmngr.model.service.RawPlayerStatsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.ProcessServiceImpl;
import net.dflmngr.model.service.impl.RawPlayerStatsServiceImpl;
import net.dflmngr.utils.DflmngrUtils;

public class RawStatsDownloaderHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultLogfile = "RoundProgress";
	String logfile;
	
	RawPlayerStatsService rawPlayerStatsService;
	ProcessService processService;
	GlobalsService globalsService;
	
	public RawStatsDownloaderHandler() {
		//PhantomJsDriverManager.getInstance().setup();
		
		rawPlayerStatsService = new RawPlayerStatsServiceImpl();
		processService = new ProcessServiceImpl();
		globalsService = new GlobalsServiceImpl();
		
		isExecutable = false;
	}
		
	public void configureLogging(String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round, String homeTeam, String awayTeam, String statsUrl, boolean includeHomeTeam, boolean includeAwayTeam, String scrapingStatus) {
		
		Process process = new Process();
		//ProcessPK processPK = new ProcessPK();
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
		
		try {
			if(!isExecutable) {
				configureLogging(defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			//processPK.setProcessId(System.getenv("DYNO"));
			//processPK.setStartTime(now);
			//process.setProcessId(processPK.getProcessId());
			if(System.getenv().containsKey("HEROKU_DYNO_ID")) {
				process.setProcessId(System.getenv("HEROKU_DYNO_ID"));
			} else {
				process.setProcessId(UUID.randomUUID().toString());
			}
			
			//process.setStartTime(processPK.getStartTime());
			process.setStartTime(now);
			process.setParams(round + " " + homeTeam + " " + awayTeam + " " + statsUrl);
			process.setStatus("Running");
			
			loggerUtils.log("info", "Creating process record: {}", process);
			
			processService.insert(process);
			
			loggerUtils.log("info", "Downloading AFL stats: round={}, homeTeam={} awayTeam={} url={}", round, homeTeam, awayTeam, statsUrl);
			
			List<RawPlayerStats> playerStats = null;
			boolean statsDownloaded = false;
			for(int i = 0; i < 5; i++) {
				loggerUtils.log("info", "Attempt {}", i);
				try {
					playerStats = downloadStats(round, homeTeam, awayTeam, statsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);
					loggerUtils.log("info", "Player stats count: {}", playerStats.size());
					if(includeHomeTeam && includeAwayTeam) {
						if(playerStats.size() >= 44) {
							statsDownloaded = true;
							break;
						}
					} else {
						if(playerStats.size() >= 22) {
							statsDownloaded = true;
							break;
						}
					}
				} catch (Exception ex) {
					loggerUtils.log("info", "Exception caught downloading stats will try again");
					loggerUtils.log("info", "Exception stacktrace={}", ExceptionUtils.getStackTrace(ex));
				}
			}
			if(statsDownloaded) {
				loggerUtils.log("info", "Saving player stats to database");
				
				
				if(includeHomeTeam) {
					rawPlayerStatsService.removeStatsForRoundAndTeam(round, homeTeam);
				}
				if(includeAwayTeam) {
					rawPlayerStatsService.removeStatsForRoundAndTeam(round, awayTeam);
				}
				rawPlayerStatsService.insertAll(playerStats, false);
				
				loggerUtils.log("info", "Player stats saved");
			} else {
				loggerUtils.log("info", "Player stats were not downloaded");
			}
			
			now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
			process.setEndTime(now);
			process.setStatus("Completed");
			
			processService.insert(process);
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
			now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
			process.setEndTime(now);
			process.setStatus("Failed");
			
			processService.insert(process);
		} finally {
			rawPlayerStatsService.close();;
			processService.close();;
			globalsService.close();;
		}
	}

	private List<RawPlayerStats> downloadStats(int round, String homeTeam, String awayTeam, String statsUrl, boolean includeHomeTeam, boolean includeAwayTeam, String scrapingStatus) throws Exception {
		
		List<RawPlayerStats> playerStats = new ArrayList<>();
		
		int webdriverTimeout = globalsService.getWebdriverTimeout();
		int webdriverWait = globalsService.getWebdriverWait();
		
		//WebDriver driver = new PhantomJSDriver();
		WebDriver driver = new HtmlUnitDriver(BrowserVersion.CHROME) {
	        @Override
	        protected WebClient newWebClient(BrowserVersion version) {
	            WebClient webClient = super.newWebClient(version);
	            webClient.getOptions().setThrowExceptionOnScriptError(false);
	            webClient.getOptions().setCssEnabled(false);
	            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
	            return webClient;
	        }
		};
		
		driver.manage().timeouts().implicitlyWait(webdriverWait, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(webdriverTimeout, TimeUnit.SECONDS);
		
		try {
			driver.get(statsUrl);
		} catch (Exception ex) {
			//if(driver.findElements(By.cssSelector("a[href='#full-time-stats']")).isEmpty()) {
			if(driver.findElements(By.id("full-time-stats")).isEmpty() && driver.findElements(By.id("live-stats")).isEmpty()) {
				driver.quit();
				throw new Exception("Error Loading page, URL:" + statsUrl, ex);
			}
		}
		
		boolean isLive = false;
		if(driver.findElements(By.id("full-time-stats")).isEmpty()) {
			isLive = true;
		}
		
		
		try {
			if(includeHomeTeam) {
			 playerStats.addAll(getStats(round, homeTeam, "h", driver, isLive, scrapingStatus));
			}
			
			if(includeAwayTeam) {
				playerStats.addAll(getStats(round, awayTeam, "a", driver, isLive, scrapingStatus));
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			driver.quit();
		}
				
		return playerStats;
	}
	
	private List<RawPlayerStats> getStats(int round, String aflTeam, String homeORaway, WebDriver driver, boolean isLive, String scrapingStatus) throws Exception {
		
		
		if(isLive) {
			driver.findElement(By.cssSelector("a[href='#live-stats']")).click();
		} else {
			driver.findElement(By.cssSelector("a[href='#full-time-stats']")).click();
		}
		
		driver.findElement(By.cssSelector("a[href='#advanced-stats']")).click();
		
		List<WebElement> statsRecs;
		List<RawPlayerStats> teamStats = new ArrayList<>();
		
		if(homeORaway.equals("h")) {
			statsRecs = driver.findElement(By.id("homeTeam-advanced")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
			loggerUtils.log("info", "Found home team stats for: round={}; aflTeam={}; ", round, aflTeam);
		} else {
			statsRecs = driver.findElement(By.id("awayTeam-advanced")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
			loggerUtils.log("info", "Found away team stats for: round={}; aflTeam={}; ", round, aflTeam);
		}
		

		
		for(WebElement statsRec : statsRecs) {
			List<WebElement> stats = statsRec.findElements(By.tagName("td"));
			
			RawPlayerStats playerStats = new RawPlayerStats();
			playerStats.setRound(round);
						
			playerStats.setName(stats.get(0).findElements(By.tagName("span")).get(1).getText());
			
			playerStats.setTeam(aflTeam);
			
			playerStats.setJumperNo(Integer.parseInt(stats.get(1).getText()));
			playerStats.setKicks(Integer.parseInt(stats.get(2).getText()));
			playerStats.setHandballs(Integer.parseInt(stats.get(3).getText()));
			playerStats.setDisposals(Integer.parseInt(stats.get(4).getText()));
			playerStats.setMarks(Integer.parseInt(stats.get(9).getText()));
			playerStats.setHitouts(Integer.parseInt(stats.get(12).getText()));
			playerStats.setFreesFor(Integer.parseInt(stats.get(17).getText()));
			playerStats.setFreesAgainst(Integer.parseInt(stats.get(18).getText()));
			playerStats.setTackles(Integer.parseInt(stats.get(19).getText()));
			playerStats.setGoals(Integer.parseInt(stats.get(23).getText()));
			playerStats.setBehinds(Integer.parseInt(stats.get(24).getText()));
			playerStats.setScrapingStatus(scrapingStatus);
			
			loggerUtils.log("info", "Player stats: {}", playerStats);
			
			teamStats.add(playerStats);
			
			if(teamStats.size() == 22) {
				break;
			}
		}
		
		return teamStats;
	}
	
	// For internal testing
	public static void main(String[] args) {
		
		int round = Integer.parseInt(args[0]);
		String homeTeam = args[1];
		String awayTeam = args[2];
		String statsUrl = args[3];
		boolean includeHomeTeam = Boolean.parseBoolean(args[4]);
		boolean includeAwayTeam = Boolean.parseBoolean(args[5]);
		String scrapingStatus = args[6];
		
		RawStatsDownloaderHandler handler = new RawStatsDownloaderHandler();
		handler.configureLogging("RawPlayerDownloader");
		handler.execute(round, homeTeam, awayTeam, statsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);
		
		System.exit(0);
	}
}
