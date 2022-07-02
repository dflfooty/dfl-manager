package net.dflmngr.handlers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class RawPlayerStatsHandler {
	private LoggingUtils loggerUtils;

	DflRoundInfoService dflRoundInfoService;
	AflFixtureService aflFixtureService;
	GlobalsService globalsService;

	boolean isExecutable;

	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "RawPlayerStatsHandler";

	String mdcKey;
	String loggerName;
	String logfile;

	public RawPlayerStatsHandler() {
		dflRoundInfoService = new DflRoundInfoServiceImpl();
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

	public void execute(int round, boolean scrapeAll) {

		try {
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			loggerUtils.log("info", "Downloading player stats for DFL round: {}", round);

			DflRoundInfo dflRoundInfo = dflRoundInfoService.get(round);

			List<AflFixture> fixturesToProcess = new ArrayList<>();
			Map<String, Integer> teamsToProcess = new HashMap<>();

			Set<String> aflGames = new HashSet<>();

			loggerUtils.log("info", "Checking for AFL rounds to download");


			for(DflRoundMapping roundMapping : dflRoundInfo.getRoundMapping()) {
				int aflRound = roundMapping.getAflRound();

				loggerUtils.log("info", "DFL round includes AFL round={}", aflRound);
				if(roundMapping.getAflGame() == 0) {
					if(scrapeAll) {
						List<AflFixture> fixtures = aflFixtureService.getAflFixturesPlayedForRound(aflRound);
						fixturesToProcess.addAll(fixtures);
					} else {
						fixturesToProcess.addAll(aflFixtureService.getFixturesToScrape());
					}
				} else {
					int aflGame = roundMapping.getAflGame();
					String team = roundMapping.getAflTeam();

					teamsToProcess.put(team, aflRound);

					AflFixture fixture = null;
					if(scrapeAll) {
						fixture = aflFixtureService.getPlayedGame(aflRound, aflGame);
					} else {
						List<AflFixture> completedFixtures = aflFixtureService.getFixturesToScrape();
						for(AflFixture aflFixture : completedFixtures) {
							if(aflGame == aflFixture.getGame()) {
								fixture = aflFixture;
								break;
							}
						}
					}

					String fixtureKey = aflRound + "-" + aflGame;

					if(fixture != null) {
						if(!aflGames.contains(fixtureKey)) {
							aflGames.add(fixtureKey);
							fixturesToProcess.add(fixture);
						}
					}
				}
			}


			if(fixturesToProcess.isEmpty()) {
				loggerUtils.log("info", "No AFL games to download stats from");
			} else {
				loggerUtils.log("info", "AFL games to download stats from: {}", fixturesToProcess);
				processFixtures(round, fixturesToProcess, teamsToProcess, scrapeAll);

				if(!scrapeAll) {
					List<AflFixture> updateFixtures = new ArrayList<>();
					for(AflFixture fixture : fixturesToProcess) {
						if(fixture.getEndTime() != null) {
							fixture.setStatsDownloaded(true);
							updateFixtures.add(fixture);
						}
					}
					if(!updateFixtures.isEmpty()) {
						loggerUtils.log("info", "AFL games final download: {}", updateFixtures);
						aflFixtureService.updateAll(updateFixtures, false);
					}
				}
			}

			dflRoundInfoService.close();
			aflFixtureService.close();
			globalsService.close();

			loggerUtils.log("info", "Player stats downaloded");

		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}

	private void processFixtures(int round, List<AflFixture> fixturesToProcess, Map<String, Integer> teamsToProcess, boolean scrapeAll) throws Exception {

		String year = globalsService.getCurrentYear();
		String statsUrl = globalsService.getAflStatsUrl();

		for (AflFixture fixture : fixturesToProcess) {
			String homeTeam = fixture.getHomeTeam();
			String awayTeam = fixture.getAwayTeam();

			String roundStr = String.format("%02d", fixture.getRound());
			String gameStr = String.format("%02d", fixture.getGame());

			String fullStatsUrl = statsUrl + "/AFL" + year + roundStr + gameStr + "/playerstats";

			loggerUtils.log("info", "AFL stats URL: {}", fullStatsUrl);

			boolean includeHomeTeam = true;
			boolean includeAwayTeam = true;

			if(teamsToProcess != null && !teamsToProcess.isEmpty()) {
			int aflRoundCheck = teamsToProcess.get(homeTeam);
				if(aflRoundCheck != fixture.getRound()) {
					includeHomeTeam = false;
				}

				aflRoundCheck = teamsToProcess.get(awayTeam);
				if(aflRoundCheck != fixture.getRound()) {
					includeAwayTeam = false;
				}
			}

			String scrapingStatus = "";
			if(scrapeAll) {
				if(fixture.isStatsDownloaded()) {
					scrapingStatus = "Finalized";
				} else {
					if(fixture.getEndTime() != null) {
						scrapingStatus = "Completed";
					} else {
						scrapingStatus = "InProgress";
					}
				}
			} else {
				if(fixture.getEndTime() != null || fixture.isStatsDownloaded()) {
					scrapingStatus = "Completed";
				} else {
					scrapingStatus = "InProgress";
				}
			}

			loggerUtils.log("info", "Scraping status={}", scrapingStatus);

			RawStatsDownloaderHandler handler = new RawStatsDownloaderHandler();
			handler.configureLogging("RawPlayerDownloader");
			handler.execute(round, homeTeam, awayTeam, fullStatsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);
		}
	}

	// For internal testing
	public static void main(String[] args) {
		Options options = new Options();

		Option roundOpt  = Option.builder("r").argName("round").hasArg().desc("round to run on").type(Number.class).required().build();
		Option onHerokuOpt = new Option("h", "use Heroku one off dyno");
		Option isFinalOpt = new Option("f", "final run");

		options.addOption(roundOpt);
		options.addOption(onHerokuOpt);
		options.addOption(isFinalOpt);

		try {
			int round = 0;
			boolean isFinal = false;

			CommandLineParser parser = new DefaultParser();
			CommandLine cli = parser.parse(options, args);

			round = ((Number)cli.getParsedOptionValue("r")).intValue();

			if(cli.hasOption("f")) {
				isFinal = true;
			}

			RawPlayerStatsHandler testing = new RawPlayerStatsHandler();
			testing.configureLogging("batch.name", "batch-logger", "RawPlayerStatsHandlerTesting");
			testing.execute(round, isFinal);
			System.exit(0);

		} catch (ParseException ex) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "RawStatsHandler", options );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
