package net.dflmngr.handlers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import net.dflmngr.exceptions.UnexpectedHtmlException;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.RawPlayerStats;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class StatsHtmlHandler {
    private LoggingUtils loggerUtils;

    boolean isExecutable;

    String defaultLogfile = "RoundProgress";
    String logfile;

    GlobalsService globalsService;

    public StatsHtmlHandler() {
        globalsService = new GlobalsServiceImpl();
        isExecutable = false;
    }

    public void configureLogging(String logfile) {
        loggerUtils = new LoggingUtils(logfile);
        this.logfile = logfile;
        isExecutable = true;
    }

    public List<RawPlayerStats> execute(int round, String homeTeam, String awayTeam, String statsUrl,
            boolean includeHomeTeam, boolean includeAwayTeam, String scrapingStatus) {

        if (!isExecutable) {
            configureLogging(defaultLogfile);
            loggerUtils.log("info", "Default logging configured");
        }

        loggerUtils.log("info", "Loading Stats HTML: round={}, homeTeam={} awayTeam={} url={}", round, homeTeam,
                awayTeam, statsUrl);
        List<RawPlayerStats> stats = downloadStats(round, homeTeam, awayTeam, statsUrl, includeHomeTeam,
                includeAwayTeam, scrapingStatus);
        loggerUtils.log("info", "Finished Loading Stats HTML");

        return stats;
    }

    private List<RawPlayerStats> downloadStats(int round, String homeTeam, String awayTeam, String statsUrl,
            boolean includeHomeTeam, boolean includeAwayTeam, String scrapingStatus) {

        List<RawPlayerStats> playerStats = new ArrayList<>();

        int webdriverTimeout = globalsService.getWebdriverTimeout();
        int webdriverWait = globalsService.getWebdriverWait();

        BrowserVersion browserVersion = new BrowserVersion.BrowserVersionBuilder(BrowserVersion.CHROME)
                .setApplicationName("DFLManager").setApplicationVersion("1.0")
                .setUserAgent("DFLManager/1.0 Stat Retriever").build();

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

        boolean useProxy = Boolean.parseBoolean(System.getenv("USE_PROXY"));

        if (useProxy) {
            loggerUtils.log("info", "Using HTTP Proxy");
            driver = new HtmlUnitDriver(browserVersion) {
                @Override
                protected WebClient newWebClient(BrowserVersion version) {
                    WebClient webClient = super.newWebClient(version);
                    webClient.getOptions().setThrowExceptionOnScriptError(false);
                    webClient.getOptions().setCssEnabled(false);
                    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

                    String fixieUrl = System.getenv("FIXIE_URL");

                    String[] fixieValues = fixieUrl.split("[/(:\\/@)/]+");
                    String fixieScheme = fixieValues[0];
                    String fixieUser = fixieValues[1];
                    String fixiePassword = fixieValues[2];
                    String fixieHost = fixieValues[3];
                    int fixiePort = Integer.parseInt(fixieValues[4]);

                    webClient.getOptions().setProxyConfig(new ProxyConfig(fixieHost, fixiePort, fixieScheme));
                    webClient.getCredentialsProvider().setCredentials(new AuthScope(fixieHost, fixiePort),
                            new UsernamePasswordCredentials(fixieUser, fixiePassword));
                    return webClient;
                }
            };
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(webdriverWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(webdriverTimeout));

        try {
            driver.get(statsUrl);
        } catch (Exception ex) {
            if (driver.findElements(By.id("full-time-stats")).isEmpty()
                    && driver.findElements(By.id("live-stats")).isEmpty()) {
                driver.quit();
                throw new UnexpectedHtmlException();
            }
        }

        try {
            if (includeHomeTeam) {
                playerStats.addAll(getStats(round, homeTeam, "h", driver, scrapingStatus));
            }

            if (includeAwayTeam) {
                playerStats.addAll(getStats(round, awayTeam, "a", driver, scrapingStatus));
            }
        } finally {
            driver.quit();
        }

        return playerStats;
    }

    private List<RawPlayerStats> getStats(int round, String aflTeam, String homeORaway, WebDriver driver, String scrapingStatus) {

        List<WebElement> statsRecs;
        List<RawPlayerStats> teamStats = new ArrayList<>();

        if (homeORaway.equals("h")) {
            statsRecs = driver.findElements(By.className("fiso-mcfootball-match-player-stats-tables__team")).get(0)
                    .findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
            loggerUtils.log("info", "Found home team stats for: round={}; aflTeam={}; ", round, aflTeam);
        } else {
            statsRecs = driver.findElements(By.className("fiso-mcfootball-match-player-stats-tables__team")).get(1)
                    .findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
            loggerUtils.log("info", "Found away team stats for: round={}; aflTeam={}; ", round, aflTeam);
        }

        for (WebElement statsRec : statsRecs) {
            List<WebElement> stats = statsRec.findElements(By.tagName("td"));

            RawPlayerStats playerStats = new RawPlayerStats();
            playerStats.setRound(round);
            playerStats.setName(stats.get(1).getText());
            playerStats.setTeam(aflTeam);
            playerStats.setJumperNo(Integer.parseInt(stats.get(0).getText()));
            playerStats.setKicks(Integer.parseInt(stats.get(4).getText()));
            playerStats.setHandballs(Integer.parseInt(stats.get(5).getText()));
            playerStats.setDisposals(Integer.parseInt(stats.get(6).getText()));
            playerStats.setMarks(Integer.parseInt(stats.get(7).getText()));
            playerStats.setHitouts(Integer.parseInt(stats.get(8).getText()));
            playerStats.setFreesFor(Integer.parseInt(stats.get(9).getText()));
            playerStats.setFreesAgainst(Integer.parseInt(stats.get(10).getText()));
            playerStats.setTackles(Integer.parseInt(stats.get(11).getText()));
            playerStats.setGoals(Integer.parseInt(stats.get(2).getText()));
            playerStats.setBehinds(Integer.parseInt(stats.get(3).getText()));
            playerStats.setScrapingStatus(scrapingStatus);

            loggerUtils.log("info", "Player stats: {}", playerStats);

            teamStats.add(playerStats);
        }

        return teamStats;
    }

    public static void main(String[] args) {

        int round = Integer.parseInt(args[0]);
        String homeTeam = args[1];
        String awayTeam = args[2];
        String statsUrl = args[3];
        boolean includeHomeTeam = Boolean.parseBoolean(args[4]);
        boolean includeAwayTeam = Boolean.parseBoolean(args[5]);
        String scrapingStatus = args[6];

        StatsHtmlHandler handler = new StatsHtmlHandler();
        handler.configureLogging("RawPlayerDownloader");

        try {
            handler.execute(round, homeTeam, awayTeam, statsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}