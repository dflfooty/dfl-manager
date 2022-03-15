package net.dflmngr.handlers;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

	public AflPlayerLoaderHtmlHandler() {
		loggerUtils = new LoggingUtils("AflPlayerLoader");

		globalsService = new GlobalsServiceImpl();
	}

	public List<AflPlayer> execute(String teamId, String url, boolean useOfficialPlayers) throws Exception {

		loggerUtils.log("info", "Loading Afl Players HTML: teamId={}, aflRound={} url={}", teamId, url);

		List<AflPlayer> aflPlayers = useOfficialPlayers
			? officialPlayerLoad(teamId, url)
			: unofficialPlayerLoad(teamId, url);

		loggerUtils.log("info", "Finished Loading Afl Players HTML");

		return aflPlayers;
	}

	private List<AflPlayer> unofficialPlayerLoad(String teamId, String url) throws Exception {

		List<AflPlayer> aflPlayers;

		InputStream teamPageInput = getInputStream(url);
		Document doc = Jsoup.parse(teamPageInput, "UTF-8", url);

		aflPlayers = loadPlayers(teamId, doc);

		return aflPlayers;
	}

	private List<AflPlayer> officialPlayerLoad(String teamId, String url) {
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

		aflPlayers = loadOfficialPlayers(teamId, driver);

		driver.quit();

		return aflPlayers;
	}

	private List<AflPlayer> loadOfficialPlayers(String teamId, WebDriver driver) {

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

            String firstName = teamPlayer.findElement(By.className("player-item__name")).getAttribute("textContent").trim();

            List<WebElement> nameChilds = teamPlayer.findElement(By.className("player-item__name")).findElements(By.xpath("./*"));
            for(WebElement child : nameChilds) {
                firstName = firstName.replaceFirst(child.getAttribute("textContent"), "").trim();
            }
            aflPlayer.setFirstName(firstName);

			String secondName = teamPlayer.findElement(By.className("player-item__last-name")).getAttribute("textContent").trim();
			aflPlayer.setSecondName(secondName);

			aflPlayer.setName(firstName + " " + secondName);
			aflPlayer.setPlayerId(aflPlayer.getTeamId() + aflPlayer.getJumperNo());

			loggerUtils.log("info", "Extraced player data: {}", aflPlayer);

            aflPlayers.add(aflPlayer);
        }

		return aflPlayers;
	}

	private List<AflPlayer> loadPlayers(String teamId, Document doc) {

		List<AflPlayer> aflPlayers = new ArrayList<>();

		Element playerContent = doc.getElementById("content-area");
		Element playerList = playerContent.getElementsByTag("table").get(1);

		int noJumper = 99;

		Elements playerRecs = playerList.getElementsByTag("tr");
		for (Element playerRec : playerRecs) {

			Elements playerRecFields = playerRec.getElementsByTag("td");

			if (!playerRecFields.isEmpty()) {
				AflPlayer aflPlayer = new AflPlayer();
				aflPlayer.setTeamId(teamId);

				String name = playerRecFields.get(0).text();
				aflPlayer.setName(name);

				String[] nameParts = name.split(" ");

				if (nameParts.length == 2) {
					aflPlayer.setFirstName(nameParts[0]);
					aflPlayer.setSecondName(nameParts[1]);
				} else if (nameParts.length == 3) {
					aflPlayer.setFirstName(nameParts[0]);
					aflPlayer.setSecondName(nameParts[2]);
				} else {
					aflPlayer.setFirstName(nameParts[0]);
					aflPlayer.setSecondName(String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length)));
				}

				String jumperNoString = playerRecFields.get(2).text();
				if (StringUtils.isNumeric(jumperNoString)) {
					aflPlayer.setJumperNo(Integer.parseInt(jumperNoString));
				} else {
					aflPlayer.setJumperNo(noJumper);
					noJumper--;
				}

				aflPlayer.setPlayerId(aflPlayer.getTeamId() + aflPlayer.getJumperNo());

				loggerUtils.log("info", "Extraced player data: {}", aflPlayer);

				aflPlayers.add(aflPlayer);
			}
		}

		return aflPlayers;
	}

	private InputStream getInputStream(String url) throws Exception {

		InputStream inputStream = null;

		boolean isStreamOpen = false;
		int maxRetries = 10;
		int retries = 0;

		while (!isStreamOpen) {
			boolean exception = false;
			try {
				inputStream = new URL(url).openStream();
			} catch (Exception ex) {
				exception = true;
				retries++;
				loggerUtils.log("info", "Failed to open team page retries {} of {}", retries, maxRetries);
				if (retries == maxRetries) {
					Exception ex2 = new Exception("Max re-tries hit failed", ex);
					throw ex2;
				}
				try {
					loggerUtils.log("info", "Waiting...");
					TimeUnit.SECONDS.sleep(5);
				} catch (Exception ex3) {
				}
			}
			if (!exception) {
				if (inputStream == null) {
					retries++;
					loggerUtils.log("info", "Failed to open team page retries {} of {}", retries, maxRetries);
					if (retries == maxRetries) {
						Exception ex = new Exception("Max re-tries hit failed");
						throw ex;
					}
				} else {
					isStreamOpen = true;
				}
			}
		}

		return inputStream;
	}

	public static void main(String[] args) {

		String teamId = args[0];
		String url = args[1];
		boolean useOfficial = Boolean.parseBoolean(args[2]);

		AflPlayerLoaderHtmlHandler handler = new AflPlayerLoaderHtmlHandler();

		try {
			List<AflPlayer> players = handler.execute(teamId, url, useOfficial);
			System.out.println(players);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

}