package net.dflmngr.handlers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflFixtureLoaderHandler {
	private LoggingUtils loggerUtils;
	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd h:mm a yyyy");
	
	GlobalsService globalsService;
	AflFixtureService aflFixtureService;
	
	public AflFixtureLoaderHandler() {
		
		loggerUtils = new LoggingUtils("AflFixtureLoader");
		
		try {
			globalsService = new GlobalsServiceImpl();
			aflFixtureService = new AflFixtureServiceImpl();
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}	
	}
	
	public void execute(List<Integer> aflRounds) throws Exception {
		
		try {
			loggerUtils.log("info", "Executing AflFixtureLoader for rounds: {}", aflRounds);
			
			List<AflFixture> allGames = new ArrayList<>();
			String currentYear = globalsService.getCurrentYear();
			
			loggerUtils.log("info", "Current year: {}", currentYear);
			
			for(Integer aflRound : aflRounds) {
				allGames.addAll(getAflRoundFixture(currentYear, aflRound));
			}
			
			loggerUtils.log("info", "Saveing data to DB");
			
			aflFixtureService.insertAll(allGames, false);
			
			loggerUtils.log("info", "AflFixtureLoader Complete");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private List<AflFixture> getAflRoundFixture(String currentYear, Integer aflRound) throws Exception {
		
		List<AflFixture> games = new ArrayList<>();
		String aflRoundStr = aflRound.toString();
		String paddedRoundNo = "";
		
		if(aflRoundStr.length() < 2) {
			paddedRoundNo = "0" + aflRoundStr;
		} else {
			paddedRoundNo = aflRoundStr;
		}
		
		List<String> aflFixtureUrlParts = globalsService.getAflFixtureUrl();
		String aflFixtureUrl = String.format(aflFixtureUrlParts.get(0), currentYear, currentYear, paddedRoundNo);
		
		loggerUtils.log("info", "AFL fixture URL: {}", aflFixtureUrl);
		
		WebDriverManager.chromedriver().setup();
		WebDriver driver = new ChromeDriver();
		
		int webdriverTimeout = globalsService.getWebdriverTimeout();
		int webdriverWait = globalsService.getWebdriverWait();
		driver.manage().timeouts().implicitlyWait(webdriverWait, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(webdriverTimeout, TimeUnit.SECONDS);
		
		driver.get(aflFixtureUrl);
		
		int retry = 1;
		while(retry <= 5) {
			loggerUtils.log("info", "Try: {}", retry);
			if(driver.findElements(By.className("match-list__details")).isEmpty()) {
				loggerUtils.log("info", "Still waiting, will try again in 5");
				Thread.sleep(5000);
			} else {
				break;
			}
			retry++;
		}
		
		List<WebElement> fixtureRows = driver.findElements(By.className("match-list__details"));
		
		int gameNo = 1;

		for(WebElement fixtureRow : fixtureRows) {
			AflFixture fixture = new AflFixture();
			fixture.setRound(aflRound);
			fixture.setGame(gameNo);

            List<WebElement> teams = fixtureRow.findElements(By.className("match-team__name"));
            
			String homeTeam = teams.get(0).getAttribute("textContent").trim();
            String awayTeam = teams.get(1).getAttribute("textContent").trim();
			fixture.setHomeTeam(homeTeam);
			fixture.setAwayTeam(awayTeam);
            
            String ground = fixtureRow.findElement(By.className("match-scheduled__venue")).getText().replaceAll("[^a-zA-Z0-9]", "");
			Map<String, String> groundData = globalsService.getGround(ground);
			fixture.setGround(groundData.get("ground"));

			long unixDateTime = Long.parseLong(fixtureRow.findElement(By.className("match-scheduled__time")).getAttribute("data-date"));
			fixture.setStartTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(unixDateTime), ZoneId.systemDefault()));

			loggerUtils.log("info", "Scraped fixture data: {}", fixture);

            games.add(fixture);

            gameNo++;
        }
		
		driver.quit();
		
		return games;
	}
	
	// For internal testing
	public static void main(String[] args) {
		try {
			
			AflFixtureLoaderHandler testing = new AflFixtureLoaderHandler();				
			List<Integer> testRounds = new ArrayList<>();
			
			for(int i = 1; i < 24; i++) {
				testRounds.add(i);
			}
			
			testing.execute(testRounds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}