package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflPlayer;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflPlayerLoaderHtmlHandler {
	private LoggingUtils loggerUtils;

	GlobalsService globalsService;

	private static final String TEXT_CONTENT_ATTRIBUTE = "textContent";

	public AflPlayerLoaderHtmlHandler() {
		loggerUtils = new LoggingUtils("AflPlayerLoader");
		globalsService = new GlobalsServiceImpl();
	}

	public List<AflPlayer> execute(String teamId, String url) {

		loggerUtils.log("info", "Loading Afl Players HTML: teamId={}, aflRound={} url={}", teamId, url);

		List<AflPlayer> aflPlayers = playerLoad(teamId, url);

		loggerUtils.log("info", "Finished Loading Afl Players HTML");

		return aflPlayers;
	}

	public void report(List<AflPlayer> players) {
		loggerUtils.log("info", "Loaded players: {}", players);
	}

	private List<AflPlayer> playerLoad(String teamId, String url) {
		List<AflPlayer> aflPlayers;

		WebDriverManager.chromedriver().setup();

		ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--headless");

		WebDriver driver = new ChromeDriver(chromeOptions);

		int webdriverTimeout = globalsService.getWebdriverTimeout();
		int webdriverWait = globalsService.getWebdriverWait();
		driver.manage().timeouts().implicitlyWait(webdriverWait, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(webdriverTimeout, TimeUnit.SECONDS);

		driver.get(url);

		aflPlayers = readPlayerHtml(teamId, driver);

		driver.quit();

		return aflPlayers;
	}

	private List<AflPlayer> readPlayerHtml(String teamId, WebDriver driver) {

		List<AflPlayer> aflPlayers = new ArrayList<>();

		List<WebElement> teamList = driver.findElements(By.className("squad-list__item"));

		int noJumper = 99;

		for(WebElement teamPlayer : teamList) {
            AflPlayer aflPlayer = new AflPlayer();
			aflPlayer.setTeamId(teamId);

			List<WebElement> hasJumper = teamPlayer.findElements(By.className("player-item__jumper-number"));
			String jumperNoString = "";
			if(!hasJumper.isEmpty()) {
				jumperNoString = teamPlayer.findElement(By.className("player-item__jumper-number")).getText();
			}
			if (StringUtils.isNumeric(jumperNoString)) {
				aflPlayer.setJumperNo(Integer.parseInt(jumperNoString));
			} else {
				aflPlayer.setJumperNo(noJumper);
				noJumper--;
			}

            String firstName = teamPlayer.findElement(By.className("player-item__name")).getAttribute(TEXT_CONTENT_ATTRIBUTE).trim();

            List<WebElement> nameChilds = teamPlayer.findElement(By.className("player-item__name")).findElements(By.xpath("./*"));
            for(WebElement child : nameChilds) {
                firstName = firstName.replaceFirst(child.getAttribute(TEXT_CONTENT_ATTRIBUTE), "").trim();
            }
            aflPlayer.setFirstName(firstName);

			String secondName = teamPlayer.findElement(By.className("player-item__last-name")).getAttribute(TEXT_CONTENT_ATTRIBUTE).trim();
			aflPlayer.setSecondName(secondName);

			aflPlayer.setName(firstName + " " + secondName);
			aflPlayer.setPlayerId(aflPlayer.getTeamId() + aflPlayer.getJumperNo());

			loggerUtils.log("info", "Extraced player data: {}", aflPlayer);

            aflPlayers.add(aflPlayer);
        }

		return aflPlayers;
	}

	public static void main(String[] args) {

		String teamId = args[0];
		String url = args[1];

		AflPlayerLoaderHtmlHandler handler = new AflPlayerLoaderHtmlHandler();

		try {
			List<AflPlayer> players = handler.execute(teamId, url);
			handler.report(players);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

}