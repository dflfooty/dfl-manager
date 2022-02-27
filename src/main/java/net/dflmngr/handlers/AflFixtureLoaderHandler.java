package net.dflmngr.handlers;

import java.time.Instant;
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
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.AflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.AflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflFixtureLoaderHandler {
	private LoggingUtils loggerUtils;
	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd h:mm a yyyy");
	
	GlobalsService globalsService;
	AflFixtureService aflFixtureService;
	AflTeamService aflTeamService;
	AflFixtureHtmlHandler aflFixtureHtmlHandler;
	
	public AflFixtureLoaderHandler() {
		
		loggerUtils = new LoggingUtils("AflFixtureLoader");
		
		try {
			globalsService = new GlobalsServiceImpl();
			aflFixtureService = new AflFixtureServiceImpl();
			aflTeamService = new AflTeamServiceImpl();
			aflFixtureHtmlHandler = new AflFixtureHtmlHandler();
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}	
	}
	
	public void execute(List<Integer> aflRounds) throws Exception {
		
		try {
			loggerUtils.log("info", "Executing AflFixtureLoader for rounds: {}", aflRounds);
			
			List<AflFixture> allGames = new ArrayList<>();

			List<String> aflFixtureUrlParts = globalsService.getAflFixtureUrl();
			
			for(Integer aflRound : aflRounds) {
				String aflFixtureUrl = aflFixtureUrlParts.get(0) + aflRound;
				allGames.addAll(aflFixtureHtmlHandler.execute(aflRound, aflFixtureUrl));
			}
			
			loggerUtils.log("info", "Saveing data to DB");
			
			//aflFixtureService.insertAll(allGames, false);
			aflFixtureService.updateLoadedFixtures(allGames);
			
			loggerUtils.log("info", "AflFixtureLoader Complete");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
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