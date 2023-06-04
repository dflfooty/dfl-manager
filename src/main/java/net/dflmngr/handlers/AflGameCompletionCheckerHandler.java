package net.dflmngr.handlers;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;

import net.dflmngr.exceptions.HtmlPageLoadException;
import net.dflmngr.exceptions.ProxyUrlException;
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
		try {
			if (!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			loggerUtils.log("info", "AflGameCompletionChecker excuting ....");

			List<AflFixture> incompleteFixtures = aflFixtureService.getIncompleteFixtures();

			if (incompleteFixtures != null && !incompleteFixtures.isEmpty()) {
				ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));

				loggerUtils.log("info", "Incomplete AFL fixtures, fixtures={}", incompleteFixtures);

				List<AflFixture> completeFixtures = new ArrayList<>();

				String year = globalsService.getCurrentYear();
				String statsUrl = globalsService.getAflStatsUrl();

				for (AflFixture fixture : incompleteFixtures) {
					String roundStr = String.format("%02d", fixture.getRound());
					String gameStr = String.format("%02d", fixture.getGame());
					String fullStatsUrl = statsUrl + "/AFL" + year + roundStr + gameStr;

					loggerUtils.log("info", "Checking for complete fixute at URL={}", fullStatsUrl);

					if (checkGame(fullStatsUrl)) {
						fixture.setEndTime(now);
						completeFixtures.add(fixture);
						loggerUtils.log("info", "Fixture complete, fixture={}", fixture);
					} else {
						loggerUtils.log("info", "Fixture not complete, fixture={}", fixture);
					}
				}

				if (!completeFixtures.isEmpty()) {
					aflFixtureService.updateAll(completeFixtures, false);
				}
			} else {
				loggerUtils.log("info", "All started AFL fixtures are complete");
			}

			aflFixtureService.close();
			globalsService.close();

			loggerUtils.log("info", "AflGameCompletionChecker comlpeted");

		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}

	private boolean checkGame(String statsUrl) {

		boolean gameCompleted = false;

		int webdriverTimeout = globalsService.getWebdriverTimeout();
		int webdriverWait = globalsService.getWebdriverWait();

		BrowserVersion browserVersion = new BrowserVersion.BrowserVersionBuilder(BrowserVersion.CHROME)
				.setApplicationName("DFLManager").setApplicationVersion("1.0")
				.setUserAgent("DFLManager/1.0 Game Completion Checker").build();

		WebDriver driver = new HtmlUnitDriver(browserVersion) {
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

					URL fixieUrl;
					try {
						fixieUrl = new URL(System.getenv("FIXIE_URL"));
					} catch (MalformedURLException e) {
						throw new ProxyUrlException();
					}
					String fixieScheme = fixieUrl.getProtocol();
					String fixieHost = fixieUrl.getHost();
					int fixiePort = fixieUrl.getPort();

					String[] userInfo = fixieUrl.getUserInfo().split(":");
					String fixieUser = userInfo[0];
					String fixiePassword = userInfo[1];

					webClient.getOptions().setProxyConfig(new ProxyConfig(fixieHost, fixiePort, fixieScheme));
					webClient.getCredentialsProvider().setCredentials(new AuthScope(fixieHost, fixiePort),
							new UsernamePasswordCredentials(fixieUser, fixiePassword));
					return webClient;
				}
			};
		}

		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(webdriverWait));
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(webdriverTimeout));

		for (int i = 1; i <= 5; i++) {
			try {
				loggerUtils.log("info", "AflGameCompletionChecker attempt: {}", i);
				driver.get(statsUrl);

				if (!driver.findElements(By.className("styles__Scoreboard-sc-14r16wm-0")).isEmpty()) {
					WebElement scorecard = driver.findElement(By.className("styles__Scoreboard-sc-14r16wm-0"));
					if (scorecard.findElement(By.className("styles__State-lxmyn6-2")).getText().equals("Full Time")) {
						gameCompleted = true;
					} else {
						gameCompleted = false;
					}
					loggerUtils.log("info", "Page loaded - Game completed: {}", gameCompleted);
					break;
				} else {
					gameCompleted = false;
					loggerUtils.log("info", "Page failed to load");
				}
			} catch (Exception ex) {
				throw new HtmlPageLoadException(statsUrl, ex);
			} finally {
				driver.quit();
			}
		}

		return gameCompleted;
	}

	public static void main(String[] args) {
		AflGameCompletionCheckerHandler testing = new AflGameCompletionCheckerHandler();
		testing.execute();
	}
}
