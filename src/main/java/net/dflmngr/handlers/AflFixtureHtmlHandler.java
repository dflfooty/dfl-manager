package net.dflmngr.handlers;

import java.time.Duration;
import java.time.Instant;
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
import io.github.bonigarcia.wdm.WebDriverManager;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.service.AflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflFixtureHtmlHandler {
    private LoggingUtils loggerUtils;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MMMM dd h:mm a yyyy");

    boolean isExecutable;

    String defaultLogfile = "AflFixuterDownload";
    String logfile;

    GlobalsService globalsService;
    AflTeamService aflTeamService;

    public AflFixtureHtmlHandler() {
        loggerUtils = new LoggingUtils("AflFixtureLoader");

        globalsService = new GlobalsServiceImpl();
        aflTeamService = new AflTeamServiceImpl();
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

        List<AflFixture> games = new ArrayList<>();

		WebDriverManager.chromedriver().setup();

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--remote-allow-origins=*");

		WebDriver driver = new ChromeDriver(chromeOptions);

		int webdriverTimeout = globalsService.getWebdriverTimeout();
		int webdriverWait = globalsService.getWebdriverWait();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(webdriverWait));
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(webdriverTimeout));

		driver.get(aflFixtureUrl);

		int retry = 1;
		while(retry <= 5) {
			loggerUtils.log("info", "Try: {}", retry);
			if(driver.findElements(By.className("match-list")).isEmpty()) {
				loggerUtils.log("info", "Still waiting, will try again in 5");
				try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
			fixture.setHomeTeam(aflTeamService.getAflTeamByName(homeTeam).getTeamId());
			fixture.setAwayTeam(aflTeamService.getAflTeamByName(awayTeam).getTeamId());
            
            String ground = fixtureRow.findElement(By.className("match-scheduled__venue")).getText().replaceAll("[^a-zA-Z0-9]", "");
			Map<String, String> groundData = globalsService.getGround(ground);
			fixture.setGround(groundData.get("ground"));

            try {
                long unixDateTime = Long.parseLong(fixtureRow.findElement(By.className("match-scheduled__time")).getAttribute("data-date"));
                fixture.setStartTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(unixDateTime), ZoneId.systemDefault()));
            } catch (NumberFormatException e) {
                loggerUtils.log("info", "Fixutre start time TBC: round={}, game={}", fixture.getRound(), fixture.getGame());
            }

			loggerUtils.log("info", "Scraped fixture data: {}", fixture);

            games.add(fixture);

            gameNo++;
        }

        driver.quit();

        return games;
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