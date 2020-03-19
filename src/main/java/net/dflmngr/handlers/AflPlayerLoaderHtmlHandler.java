package net.dflmngr.handlers;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflPlayer;

public class AflPlayerLoaderHtmlHandler {
    private LoggingUtils loggerUtils;

    public AflPlayerLoaderHtmlHandler() {
        loggerUtils = new LoggingUtils("AflPlayerLoader");
    }

    public List<AflPlayer> execute(String teamId, String url) throws Exception {

        loggerUtils.log("info", "Loading Afl Players HTML: teamId={}, aflRound={} url={}", teamId, url);

        List<AflPlayer> aflPlayers;

        InputStream teamPageInput = getInputStream(url);
        Document doc = Jsoup.parse(teamPageInput, "UTF-8", url);

        aflPlayers = loadPlayers(teamId, doc);

        loggerUtils.log("info", "Finished Loading Afl Players HTML");

        return aflPlayers;
    }

    private List<AflPlayer> loadPlayers(String teamId, Document doc) {

		List<AflPlayer> aflPlayers = new ArrayList<>();

		Element playerList = doc.getElementsByClass("squad-list").get(0);

		int noJumper = 99;

		Elements playerRecs = playerList.getElementsByTag("li");
		for(Element playerRec : playerRecs) {

			AflPlayer aflPlayer = new AflPlayer();

			aflPlayer.setTeamId(teamId);

			String jumperNoString = playerRec.getElementsByClass("player-item__jumper-number").get(0).text();
			if(StringUtils.isNumeric(jumperNoString)) {
				aflPlayer.setJumperNo(Integer.parseInt(jumperNoString));
			} else {
				aflPlayer.setJumperNo(noJumper);
				noJumper--;
			}

            aflPlayer.setPlayerId(aflPlayer.getTeamId()+aflPlayer.getJumperNo());

            String playerFirstName = playerRec.getElementsByClass("player-item__name").get(0).ownText().trim();
            String playerSecondName = playerRec.getElementsByClass("player-item__last-name").get(0).ownText().trim();

			aflPlayer.setName(playerFirstName + " " + playerSecondName);
			aflPlayer.setFirstName(playerFirstName);
			aflPlayer.setSecondName(playerSecondName);

			//aflPlayer.setHeight(Integer.parseInt(playerData.get(2).text()));
			//aflPlayer.setWeight(Integer.parseInt(playerData.get(3).text()));
			//aflPlayer.setDob(df.parse(playerData.get(4).text()));

			loggerUtils.log("info", "Extraced player data: {}", aflPlayer);

			aflPlayers.add(aflPlayer);
		}

		return aflPlayers;

    }

    private InputStream getInputStream(String url) throws Exception {

		InputStream inputStream = null;

		boolean isStreamOpen = false;
		int maxRetries = 10;
		int retries = 0;

		while(!isStreamOpen) {
			boolean exception = false;
			try {
				inputStream = new URL(url).openStream();
			} catch (Exception ex) {
				exception = true;
				retries++;
				loggerUtils.log("info", "Failed to open team page retries {} of {}", retries, maxRetries);
				if(retries == maxRetries) {
					Exception ex2 = new Exception("Max re-tries hit failed", ex);
					throw ex2;
				}
				try {
					loggerUtils.log("info", "Waiting...");
					TimeUnit.SECONDS.sleep(5);
				} catch (Exception ex3) {}
			}
			if(!exception) {
				if(inputStream == null) {
					retries++;
					loggerUtils.log("info", "Failed to open team page retries {} of {}", retries, maxRetries);
					if(retries == maxRetries) {
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

        AflPlayerLoaderHtmlHandler handler = new AflPlayerLoaderHtmlHandler();

        try {
            List<AflPlayer> players = handler.execute(teamId, url);
            System.out.println(players);
        } catch (Exception e) {
            e.printStackTrace();
        }

		System.exit(0);
	}

}