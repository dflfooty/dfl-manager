package net.dflmngr.handlers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import net.dflmngr.exceptions.AflFixtureException;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.service.AflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflFixtureHtmlHandler {
    private LoggingUtils loggerUtils;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MMMM d h:mma yyyy");

    boolean isExecutable;

    String defaultLogfile = "AflFixuterDownload";
    String logfile;

    GlobalsService globalsService;
    AflTeamService aflTeamService;

    String currentYear;
    String defaultTimezone;

    static final String HTML_CLASS_STRING = "class";

    public AflFixtureHtmlHandler() {
        loggerUtils = new LoggingUtils("AflFixtureLoader");

        globalsService = new GlobalsServiceImpl();
        aflTeamService = new AflTeamServiceImpl();

        currentYear = globalsService.getCurrentYear();
        defaultTimezone = globalsService.getGroundTimeZone("default");
    }

    public void configureLogging(String logfile) {
        loggerUtils = new LoggingUtils(logfile);
        this.logfile = logfile;
        isExecutable = true;
    }

    public List<AflFixture> execute(Integer aflRound, String aflFixtureUrl) {

        loggerUtils.log("info", "Loading Afl Fixture HTML: aflRound={} url={}", aflRound, aflFixtureUrl);

        List<AflFixture> games = download(aflRound, aflFixtureUrl);

        loggerUtils.log("info", "Finished Loading Afl Fixture HTML");

        return games;
    }

    private List<AflFixture> download(Integer aflRound, String aflFixtureUrl) {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--remote-allow-origins=*");

        int webdriverTimeout = globalsService.getWebdriverTimeout();
		int webdriverWait = globalsService.getWebdriverWait();
        chromeOptions.setImplicitWaitTimeout(Duration.ofSeconds(webdriverWait));
        chromeOptions.setPageLoadTimeout(Duration.ofSeconds(webdriverTimeout));

		WebDriver driver = new ChromeDriver(chromeOptions);

		driver.get(aflFixtureUrl);

        List<AflFixture> games = null;

		int retry = 1;
		while(retry <= 5) {
			loggerUtils.log("info", "Try: {}", retry);
			if(driver.findElements(By.id("main-content")).isEmpty()) {
				loggerUtils.log("info", "Still waiting, will try again in 5");
				try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
			} else {
                try {
                    WebElement fixtureContent = driver.findElements(By.id("main-content")).get(0);
		            games = getFixtureData(aflRound, fixtureContent, aflFixtureUrl);
                } catch (Exception ex) {
                    loggerUtils.logException("Error loading AFL fxiture", ex);
                }
				if(games != null && !games.isEmpty()) {
                    break;
                }
			}
			retry++;
		}

        driver.quit();

        return games;
    }

    List<AflFixture> getFixtureData(int aflRound, WebElement fixtureContent, String aflFixtureUrl) {
        List<AflFixture> games = new ArrayList<>();
        
        List<WebElement> fixtureRows = getFixtureRows(fixtureContent, aflFixtureUrl);
		
		int gameNo = 1;
        String date = "";

		for(WebElement fixtureRow : fixtureRows) {
            if(fixtureRow.getAttribute(HTML_CLASS_STRING).contains("fixtures__date-header")) {
                date = fixtureRow.getText().trim();
            } else if(fixtureRow.getAttribute(HTML_CLASS_STRING).contains("fixtures__item")) {
                AflFixture fixture = new AflFixture();
                fixture.setRound(aflRound);
                fixture.setGame(gameNo);

                List<WebElement> teams = fixtureRow.findElements(By.className("fixtures__match-team-name"));
                
                String homeTeam = teams.get(0).getAttribute("textContent");
                String awayTeam = teams.get(1).getAttribute("textContent");
                fixture.setHomeTeam(aflTeamService.getAflTeamByName(homeTeam).getTeamId());
                fixture.setAwayTeam(aflTeamService.getAflTeamByName(awayTeam).getTeamId());
                
                String ground = fixtureRow.findElement(By.className("fixtures__match-venue")).getText()
                                    .split(",")[0].replaceAll("[^a-zA-Z0-9]", "");
                Map<String, String> groundData = globalsService.getGround(ground);
                fixture.setGround(groundData.get("ground"));
                fixture.setTimezone(groundData.get("timezone"));

                String timeWithTZ = fixtureRow.findElement(By.className("fixtures__status-label")).getText();
                if(timeWithTZ.equalsIgnoreCase("TBC")) {
                    loggerUtils.log("info", "Fixutre start time TBC: round={}, game={}", fixture.getRound(), fixture.getGame());
                } else {
                    String time = timeWithTZ.split("\n")[0].toUpperCase();
                    String dateTimeString = date + " " + time + " " + currentYear;
                    System.out.println("#### " + dateTimeString + " ####");
                    try {
                        ZonedDateTime localStart = LocalDateTime.parse((dateTimeString), formatter).atZone(ZoneId.of(defaultTimezone));
                        fixture.setStartTime(localStart);
                    } catch (Exception ex) {
                        throw new AflFixtureException(aflFixtureUrl, ex);
                    }
                }

                loggerUtils.log("info", "Scraped fixture data: {}", fixture);

                games.add(fixture);

                gameNo++;
            } else if(fixtureRow.getAttribute(HTML_CLASS_STRING).contains("fixtures__bye-fixtures")) {
                //ignore
            } else {
                throw new AflFixtureException(aflFixtureUrl, fixtureRow.getAttribute(HTML_CLASS_STRING));
            }
        }

        return games;
    }

    List<WebElement> getFixtureRows(WebElement fixtureContent, String aflFixtureUrl) {
        List<WebElement> contents = fixtureContent.findElements(By.className("wrapper"));
        List<WebElement> fixtureRows = null;
        for(WebElement content : contents) {
            if(content.getAttribute(HTML_CLASS_STRING).equals("wrapper")) {
                fixtureRows = content.findElements(By.xpath("./*"));
            }
            if(fixtureRows != null && !fixtureRows.isEmpty()) {
                break;
            }
        }
        if(fixtureRows == null || fixtureRows.isEmpty()) {
            throw new AflFixtureException(aflFixtureUrl);
        }

        return fixtureRows;
    }

    public void report(List<AflFixture> fixtures) {
        for(AflFixture fixture: fixtures) {
            loggerUtils.log("info", "Fixture: {}", fixture);
        }
    }

    public static void main(String[] args) {

        int round = Integer.parseInt(args[0]);
        String fixtureUrl = args[1];

        AflFixtureHtmlHandler handler = new AflFixtureHtmlHandler();
        handler.configureLogging("AflFixtureDownloader");

        List<AflFixture> fixtures =  handler.execute(round, fixtureUrl);
        handler.report(fixtures);

        System.exit(0);
    }
}