package net.dflmngr.handlers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.utils.DflmngrUtils;

public class AflGameCompletionCheckerHandler {
	private LoggingUtils loggerUtils;
	
	private AflFixtureService aflFixtureService;
	private GlobalsService globalsService;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "AflGameCompletionChecker";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	public AflGameCompletionCheckerHandler() {
		aflFixtureService = new AflFixtureServiceImpl();
		globalsService = new GlobalsServiceImpl();
		
		isExecutable = false;
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute() {
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "AflGameCompletionChecker excuting ....");
		
			List<AflFixture> incompleteFixtures = aflFixtureService.getIncompleteFixtures();
			
			if(incompleteFixtures != null && !incompleteFixtures.isEmpty()) {
				ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
			
				loggerUtils.log("info", "Incomplete AFL fixtures, fixtures={}", incompleteFixtures);
				
				List<AflFixture> completeFixtures = new ArrayList<>();
				
				String year = globalsService.getCurrentYear();
				String statsUrl = globalsService.getAflStatsUrl();
				
				for(AflFixture fixture : incompleteFixtures) {
					String homeTeam = fixture.getHomeTeam();
					String awayTeam = fixture.getAwayTeam();
					String aflRound = Integer.toString(fixture.getRound());
					
					String fullStatsUrl = statsUrl + "/" + year + "/" + aflRound + "/" + homeTeam.toLowerCase() + "-v-" + awayTeam.toLowerCase();
					
					loggerUtils.log("info", "Checking for complete fixute at URL={}", fullStatsUrl);
					
					if(checkGame(fullStatsUrl)) {
						loggerUtils.log("info", "Fixture complete, fixture={}", fixture);
						fixture.setEndTime(now);
						completeFixtures.add(fixture);
					}
				}
				
				if(!completeFixtures.isEmpty()) {
					aflFixtureService.updateAll(completeFixtures, false);
				}
			} else {
				loggerUtils.log("info", "All started AFL fixtures are complete");
			}
				
			aflFixtureService.close();
			globalsService.close();
			
			loggerUtils.log("info", "AflGameCompletionChecker comlpeted");
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private boolean checkGame(String statsUrl) throws Exception {
		
		boolean gameCompleted;
		
		int webdriverTimeout = globalsService.getWebdriverTimeout();
		int webdriverWait = globalsService.getWebdriverWait();
		
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
			
			if(driver.findElements(By.cssSelector("a[href='#full-time-stats']")).isEmpty()) {
				gameCompleted = false;
			} else {
				gameCompleted = true;
			}
		} catch (Exception ex) {
			throw new Exception("Error Loading page, URL:" + statsUrl, ex);
		} finally {
			driver.quit();
		}
		
		return gameCompleted;
	}
	
	public static void main(String[] args) {
		AflGameCompletionCheckerHandler testing = new AflGameCompletionCheckerHandler();							
		testing.execute();
	}
}
