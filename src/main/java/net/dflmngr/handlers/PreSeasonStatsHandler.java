package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflPreseasonScores;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.DflPreseasonScoresService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflPreseasonScoresServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.utils.DflmngrUtils;

public class PreSeasonStatsHandler {
	
	private LoggingUtils loggerUtils;
	
	DflPlayerService dflPlayerService;
	GlobalsService globalsService;
	DflPreseasonScoresService dflPreseasonScoresService;
	
	boolean isExecutable;
		
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "PreSeasonStats";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	public PreSeasonStatsHandler() {		
		dflPlayerService = new DflPlayerServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflPreseasonScoresService = new DflPreseasonScoresServiceImpl();
		
		isExecutable = false;
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round) {
		
		if(!isExecutable) {
			configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
			loggerUtils.log("info", "Default logging configured");
		}
		
		loggerUtils.log("info", "Downloading Pre-season Stats");
		
		List<String> preSeasonStatsUrls = getPreSeasonStatsUrls(round);
		
		for(String url : preSeasonStatsUrls) {
			Map<String, Integer> stats = getPreSeasonStats(url);
			
			String teams = url.split("/")[7];
			String homeTeam = teams.split("-")[0];
			String awayTeam = teams.split("-")[2];
			
			List<DflPlayer> players = dflPlayerService.getByTeam(DflmngrUtils.aflDflTeamMap.get(homeTeam.toUpperCase()));
			players.addAll(dflPlayerService.getByTeam(DflmngrUtils.aflDflTeamMap.get(awayTeam.toUpperCase())));
			
			List<DflPreseasonScores> preseasonScores = calculatePreseasonScore(round, players, stats);
			dflPreseasonScoresService.insertAll(preseasonScores, false);
		}
		
		loggerUtils.log("info", "Player Pre-season stats saved");
	}
	
	private List<String> getPreSeasonStatsUrls(int round) {
		List<String> statsUrls = new ArrayList<>();
		
		String preSeasonFixtureUrl = globalsService.getPreSeasonFixtureUrl();

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
		
		driver.get(preSeasonFixtureUrl);
		
		List<WebElement> fixtures = driver.findElement(By.className("fixture")).findElements(By.tagName("tr"));
		
		for(WebElement fixture : fixtures) {
			
			List<WebElement> gameRow = fixture.findElements(By.tagName("td"));
						
			if(gameRow.size() >= 4) {
				String url = gameRow.get(3).findElement(By.tagName("a")).getAttribute("href");

				loggerUtils.log("info", "Found game URL: {}", url);
				
				int urlRound = Integer.parseInt(url.split("/")[6]);
				if(urlRound == round) {
					loggerUtils.log("info", "Adding URL: {}", url);
					statsUrls.add(url);
				}
			}
		}
		
		driver.quit();
		
		return statsUrls;
	}
	
	private Map<String, Integer> getPreSeasonStats(String url) {
		
		Map<String, Integer> allStats = new HashMap<>();
		
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
		
		
		driver.get(url);
					
		String roundStr = url.split("/")[6];
		String teams = url.split("/")[7];
		String homeTeam = teams.split("-")[0];
		String awayTeam = teams.split("-")[2];
		
		driver.findElement(By.cssSelector("a[href='#full-time-stats']")).click();
		driver.findElement(By.cssSelector("a[href='#advanced-stats']")).click();
		
		List<WebElement> homeStatsRecs = driver.findElement(By.id("homeTeam-advanced")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
		loggerUtils.log("info", "Found home team stats for: round={}; aflTeam={}; ", roundStr, homeTeam);
		List<WebElement> awayStatsRecs = driver.findElement(By.id("awayTeam-advanced")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
		loggerUtils.log("info", "Found away team stats for: round={}; aflTeam={}; ", roundStr, awayTeam);
		
		for(WebElement statsRec : homeStatsRecs) {
			List<WebElement> stats = statsRec.findElements(By.tagName("td"));
			
			String key = homeTeam + stats.get(1).getText();
			
			String name = stats.get(0).findElements(By.tagName("span")).get(1).getText();
			
			int disposals = Integer.parseInt(stats.get(4).getText());
			int marks = Integer.parseInt(stats.get(9).getText());
			int hitouts = Integer.parseInt(stats.get(12).getText());
			int freesFor = Integer.parseInt(stats.get(17).getText());
			int freesAgainst = Integer.parseInt(stats.get(18).getText());
			int tackles = Integer.parseInt(stats.get(19).getText());
			int goals = Integer.parseInt(stats.get(23).getText()) + Integer.parseInt(stats.get(24).getText());
			
			int total = disposals + marks + hitouts + freesFor + (-freesAgainst) + tackles + (goals * 3);
			
			
			loggerUtils.log("info", "Player stats: name={}, disposals={}, marks={} hitouts={}, freesFor{}, freesAgainst={}, tackles={}, goals={}, total={}",
						name, disposals, marks, hitouts, freesFor, freesAgainst, tackles, goals, total);
			
			allStats.put(key, total);
		}
		
		for(WebElement statsRec : awayStatsRecs) {
			List<WebElement> stats = statsRec.findElements(By.tagName("td"));
			
			String key = awayTeam + stats.get(1).getText();
			
			String name = stats.get(0).findElements(By.tagName("span")).get(1).getText();
			
			int disposals = Integer.parseInt(stats.get(4).getText());
			int marks = Integer.parseInt(stats.get(9).getText());
			int hitouts = Integer.parseInt(stats.get(12).getText());
			int freesFor = Integer.parseInt(stats.get(17).getText());
			int freesAgainst = Integer.parseInt(stats.get(18).getText());
			int tackles = Integer.parseInt(stats.get(19).getText());
			int goals = Integer.parseInt(stats.get(23).getText()) + Integer.parseInt(stats.get(24).getText());
			
			int total = disposals + marks + hitouts + freesFor + (-freesAgainst) + tackles + (goals * 3);
			
			
			loggerUtils.log("info", "Player stats: name={}, disposals={}, marks={} hitouts={}, freesFor{}, freesAgainst={}, tackles={}, goals={}, total={}",
						name, disposals, marks, hitouts, freesFor, freesAgainst, tackles, goals, total);
			
			allStats.put(key, total);
		}
					
		driver.quit();
		
		return allStats;
		
	}
	
	List<DflPreseasonScores> calculatePreseasonScore(int round, List<DflPlayer> players, Map<String, Integer> stats) {
		
		List<DflPreseasonScores> preseasonScores = new ArrayList<>();
		
	
		for(DflPlayer player : players) {
			
			loggerUtils.log("info", "Handling player: player_id={}, afl_player_id={}", player.getPlayerId(), player.getAflPlayerId());
			
			Integer total = stats.get(player.getAflPlayerId());
			
			DflPreseasonScores score = new DflPreseasonScores();
			score.setPlayerId(player.getPlayerId());
			score.setRound(round);
			
			if(total != null) {
				score.setScore(total);
			}

			
			loggerUtils.log("info", "Player scores: {}", score);
			
			preseasonScores.add(score);
		}
		
		return preseasonScores;
	}
	
	
	public static void main(String[] args) {
		PreSeasonStatsHandler testing = new PreSeasonStatsHandler();
		testing.configureLogging("batch.name", "batch-logger", "PreSeasonStats");
		testing.execute(Integer.parseInt(args[0]));
	}
	

}
