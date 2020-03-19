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
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflFixtureHtmlHandler {
    private LoggingUtils loggerUtils;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MMMM dd h:mm a yyyy");

    GlobalsService globalsService;

    public AflFixtureHtmlHandler() {
        loggerUtils = new LoggingUtils("AflFixtureLoader");

        globalsService = new GlobalsServiceImpl();
    }


    public List<AflFixture> execute(String currentYear, Integer aflRound, String aflFixtureUrl) throws Exception {

        loggerUtils.log("info", "Loading Afl Fixture HTML: currentYear={}, aflRound={} url={}", currentYear, aflRound, aflFixtureUrl);

        List<AflFixture> games = download(currentYear, aflRound, aflFixtureUrl);

        loggerUtils.log("info", "Finished Loading Afl Fixture HTML");

        return games;
    }

    private List<AflFixture> download(String currentYear, Integer aflRound, String aflFixtureUrl) throws Exception {

        List<AflFixture> games = new ArrayList<AflFixture>();

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
			if(driver.findElements(By.className("match-list")).isEmpty()) {
				loggerUtils.log("info", "Still waiting, will try again in 5");
				Thread.sleep(5000);
			} else {
				break;
			}
			retry++;
		}


		List<WebElement> fixtureRows = driver.findElements(By.className("match-list"));

		String dateString = "";
		int gameNo = 1;

		for(WebElement fixtureRow : fixtureRows) {

			if(!fixtureRow.findElements(By.className("match-list__group-date")).isEmpty()) {
                String date = fixtureRow.findElement(By.className("match-list__group-date")).getText().trim();
                String day = fixtureRow.findElement(By.className("match-list__group-date")).findElement(By.tagName("spane")).getText().trim();
				dateString = day + " " + date;
			} else {
                AflFixture fixture = new AflFixture();
                fixture.setRound(aflRound);
                fixture.setGame(gameNo);

                WebElement gameData = fixtureRow.findElement(By.className("match-list__details"));

                String homeTeam = gameData.findElements(By.className("match-team__name")).get(0).getText().trim();
                String awayTeam = gameData.findElements(By.className("match-team__name")).get(1).getText().trim();

                fixture.setHomeTeam(globalsService.getAflTeamMap(homeTeam));
                fixture.setAwayTeam(globalsService.getAflTeamMap(awayTeam));

                String timeString = gameData.findElement(By.className("match-scheduled__time")).getText().trim();
                String timeAmPmString = gameData.findElement(By.className("match-scheduled__time-ampm")).getText().trim();
                String dateTimeString = dateString + " " + timeString + " " + timeAmPmString + " " + currentYear;

                String ground = gameData.findElement(By.className("match-scheduled__venue")).getText().trim();
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
		}

        driver.quit();

        return games;
    }
}