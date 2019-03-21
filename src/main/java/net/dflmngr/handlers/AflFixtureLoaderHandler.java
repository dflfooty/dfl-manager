package net.dflmngr.handlers;

import java.time.LocalDateTime;
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
	
	//SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM dd h:mma yyyy");
	//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd h:mm a yyyy");
	
	GlobalsService globalsService;
	AflFixtureService aflFixtureService;
	
	
	
	public AflFixtureLoaderHandler() {
		
		//loggerUtils = new LoggingUtils("batch-logger", "batch.name", "AflFixtureLoader");
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
			
			List<AflFixture> allGames = new ArrayList<AflFixture>();
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
		
		List<AflFixture> games = new ArrayList<AflFixture>();
		String aflRoundStr = aflRound.toString();
		String paddedRoundNo = "";
		
		if(aflRoundStr.length() < 2) {
			paddedRoundNo = "0" + aflRoundStr;
		} else {
			paddedRoundNo = aflRoundStr;
		}
		
		
		List<String> aflFixtureUrlParts = globalsService.getAflFixtureUrl();
		//String aflFixtureUrl = aflFixtureUrlParts.get(0) + aflFixtureUrlParts.get(1) + currentYear + aflFixtureUrlParts.get(2) + paddedRoundNo + aflFixtureUrlParts.get(3);
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
			if(driver.findElements(By.className("o-fixture-match-wrapper")).isEmpty()) {
				loggerUtils.log("info", "Still waiting, will try again in 5");
				Thread.sleep(5000);
			} else {
				break;
			}
			retry++;
		}
		
		
		List<WebElement> fixtureRows = driver.findElements(By.className("o-fixture-match-wrapper"));
		
		String dateString = "";
		int gameNo = 1;
		
		for(WebElement fixtureRow : fixtureRows) {
			
			if(!fixtureRow.findElements(By.tagName("h4")).isEmpty()) {
				dateString = fixtureRow.findElements(By.tagName("h4")).get(0).getText().trim();
			}
		
			AflFixture fixture = new AflFixture();
			fixture.setRound(aflRound);
			fixture.setGame(gameNo);
			
			List<WebElement> gameData = fixtureRow.findElements(By.className("o-fixture-state-wrapper")).get(0).findElements(By.tagName("a"));
			
			String gameUrl = gameData.get(0).getAttribute("href");
			String[] teams = gameData.get(0).getAttribute("href").substring(gameUrl.lastIndexOf("/") + 1, gameUrl.length()).split("-");
			
			fixture.setHomeTeam(teams[0].toUpperCase());
			fixture.setAwayTeam(teams[2].toUpperCase());
			
			String timeString = gameData.get(0).findElements(By.className("c-fixture-timer")).get(0).getText().trim();
			String dateTimeString = dateString + " " + timeString.substring(0, timeString.lastIndexOf(" ")) + " " + currentYear;
			
			String ground = gameData.get(1).findElements(By.className("o-fixture-icon-title-wrapper")).get(0).getText().trim();
			Map<String, String> groundData = globalsService.getGround(ground);
			
			fixture.setGround(groundData.get("ground"));
						
			String timezone = groundData.get("timezone");
			String defaultTimezone = globalsService.getGroundTimeZone("default");
			
			ZonedDateTime localStart = LocalDateTime.parse((dateTimeString), formatter).atZone(ZoneId.of(timezone));
			
			if(timezone.equals(defaultTimezone)) {
				fixture.setStartTime(localStart);
			} else {
				ZonedDateTime defualtStart = localStart.withZoneSameInstant(ZoneId.of(defaultTimezone));
				fixture.setStartTime(defualtStart);
			}
							
			fixture.setTimezone(timezone);
			
			loggerUtils.log("info", "Scraped fixture data: {}", fixture);
			
			games.add(fixture);
			
			gameNo++;
		}
		
		driver.quit();
		
		/*
		Document doc = Jsoup.parse(new URL(aflFixtureUrl).openStream(), "UTF-8", aflFixtureUrl);
		
		Elements scripts = doc.getElementsByTag("script2");
		Iterator<Element> scriptElements = scripts.listIterator();
		
		String json = "";
		
		if(scriptElements.hasNext()) {
			while(scriptElements.hasNext()) {
				Element script = scriptElements.next();
				
				String scriptText = script.text();
				if(scriptText.contains("window.byClubData")) {
					json = scriptText.substring(scriptText.indexOf("{"), scriptText.lastIndexOf("}")+1);
				}
			}
		}
		
		
		JsonObject jsonObject = (JsonObject) Jsoner.deserialize(json);
		List<JsonObject> jsonFixtures = jsonObject.getCollection("fixtures");
		
		for(JsonObject jsonFixture : jsonFixtures) {
			Map<String, String> jsonHomeTeam = jsonFixture.getMap("homeTeam");
			Map<String, String> jsonAwayTeam = jsonFixture.getMap("awayTeam");
			Map<String, Object> jsonMatch = jsonFixture.getMap("match");
			JsonArray jsonStartTimes = (JsonArray) jsonMatch.get("startDateTimes");
		
			String dateTimeString = "";
			
			Iterator<Object> iter = jsonStartTimes.iterator();
			
			
			while(iter.hasNext()) {
				JsonObject startTime = (JsonObject) iter.next();
				String tz = startTime.getString("name");
				if(tz.equalsIgnoreCase("venue")) {
					dateTimeString = startTime.getString("date") + " " + startTime.getString("time");;
				}
			}
			
			AflFixture fixture = new AflFixture();
			fixture.setRound(aflRound);
			
			String matchId = (String) jsonMatch.get("matchId");
			int gameNo = Integer.parseInt(matchId.substring(matchId.length()-2));
			fixture.setGame(gameNo);
			
			
			fixture.setHomeTeam(jsonHomeTeam.get("teamAbbr"));
			fixture.setAwayTeam(jsonAwayTeam.get("teamAbbr"));
			
			String ground = (String) jsonMatch.get("venueAbbr");
			fixture.setGround(ground);
			String timezone = globalsService.getGroundTimeZone(ground);
			String defaultTimezone = globalsService.getGroundTimeZone("default");
			
			ZonedDateTime localStart = LocalDateTime.parse((dateTimeString), formatter).atZone(ZoneId.of(timezone));
			
			if(timezone.equals(defaultTimezone)) {
				fixture.setStartTime(localStart);
			} else {
				ZonedDateTime defualtStart = localStart.withZoneSameInstant(ZoneId.of(defaultTimezone));
				fixture.setStartTime(defualtStart);
			}
							
			fixture.setTimezone(timezone);
			
			loggerUtils.log("info", "Scraped fixture data: {}", fixture);
			
			games.add(fixture);
		}
		*/		
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