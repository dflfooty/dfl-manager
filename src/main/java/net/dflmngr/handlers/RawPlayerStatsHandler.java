package net.dflmngr.handlers;

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

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.ProcessService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.ProcessServiceImpl;

public class RawPlayerStatsHandler {
	private LoggingUtils loggerUtils;

	DflRoundInfoService dflRoundInfoService;
	AflFixtureService aflFixtureService;
	GlobalsService globalsService;
	ProcessService processService;

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
		processService = new ProcessServiceImpl();

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
			setup();

			loggerUtils.log("info", "Downloading player stats for DFL round: {}", round);

			DflRoundInfo dflRoundInfo = dflRoundInfoService.get(round);

			List<AflFixture> fixturesToProcess = new ArrayList<>();
			Map<String, Integer> teamsToProcess = new HashMap<>();

			loggerUtils.log("info", "Checking for AFL rounds to download");

			for(DflRoundMapping roundMapping : dflRoundInfo.getRoundMapping()) {
				int aflRound = roundMapping.getAflRound();

				loggerUtils.log("info", "DFL round includes AFL round={}", aflRound);

				fixturesToProcess.addAll(getFixturesToDownload(roundMapping, scrapeAll));

				if(roundMapping.getAflGame() > 0) {
					String team = roundMapping.getAflTeam();
					teamsToProcess.put(team, aflRound);
				}
			}

			if(fixturesToProcess.isEmpty()) {
				loggerUtils.log("info", "No AFL games to download stats from");
			} else {
				loggerUtils.log("info", "AFL games to download stats from: {}", fixturesToProcess);
				processFixtures(round, fixturesToProcess, teamsToProcess, scrapeAll);

				if(!scrapeAll) {
					updateFixtures(fixturesToProcess);
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

	private void setup() {
		if(!isExecutable) {
			configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
			loggerUtils.log("info", "Default logging configured");
		}
	}

	private List<AflFixture> getFixturesToDownload(DflRoundMapping roundMapping, boolean scrapeAll) throws Exception {
		
		int aflRound = roundMapping.getAflRound();
		List<AflFixture> fixturesToProcess = new ArrayList<>();
		Set<String> aflGames = new HashSet<>();

		if(roundMapping.getAflGame() == 0) {
			if(scrapeAll) {
				fixturesToProcess.addAll(aflFixtureService.getAflFixturesPlayedForRound(aflRound));
			} else {
				fixturesToProcess.addAll(aflFixtureService.getFixturesToScrape());
			}
		} else {
			int aflGame = roundMapping.getAflGame();

			AflFixture fixture = getCompletedFixture(aflRound, aflGame, scrapeAll);

			String fixtureKey = aflRound + "-" + aflGame;

			if(fixture != null && !aflGames.contains(fixtureKey)) {
				aflGames.add(fixtureKey);
				fixturesToProcess.add(fixture);
			}
		}

		return fixturesToProcess;
	}

	private AflFixture getCompletedFixture(int aflRound, int aflGame, boolean scrapeAll) throws Exception {
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

		return fixture;
	}

	private void processFixtures(int round, List<AflFixture> fixturesToProcess, Map<String, Integer> teamsToProcess, boolean scrapeAll) {

		String year = globalsService.getCurrentYear();
		String statsUrl = globalsService.getAflStatsUrl();

		for (AflFixture fixture : fixturesToProcess) {
			String homeTeam = fixture.getHomeTeam();
			String awayTeam = fixture.getAwayTeam();

			String roundStr = String.format("%02d", fixture.getRound());
			String gameStr = String.format("%02d", fixture.getGame());

			String fullStatsUrl = statsUrl + "/AFL" + year + roundStr + gameStr + "/playerstats";

			loggerUtils.log("info", "AFL stats URL: {}", fullStatsUrl);

			boolean includeHomeTeam = includeTeam(homeTeam, teamsToProcess, fixture);
			boolean includeAwayTeam = includeTeam(awayTeam, teamsToProcess, fixture);

			String scrapingStatus = getScrappingStauts(scrapeAll, fixture);

			loggerUtils.log("info", "Scraping status={}", scrapingStatus);

			loggerUtils.log("info", "Running locally ... ");
			RawStatsDownloaderHandler handler = new RawStatsDownloaderHandler();
			handler.configureLogging("RawPlayerDownloader");
			handler.execute(round, homeTeam, awayTeam, fullStatsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);
		}
	}

	private void updateFixtures(List<AflFixture> fixturesToProcess) {
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

	private boolean includeTeam(String team, Map<String, Integer> teamsToProcess, AflFixture fixture) {
		if(teamsToProcess != null && !teamsToProcess.isEmpty()) {
			return teamsToProcess.get(team) != fixture.getRound();
		}

		return true;
	}
	
	private String getScrappingStauts(boolean scrapeAll, AflFixture fixture) {
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

		return scrapingStatus;
	}

	// For internal testing
	public static void main(String[] args) {
		Options options = new Options();

		Option roundOpt  = Option.builder("r").argName("round").hasArg().desc("round to run on").type(Number.class).required().build();
		Option isFinalOpt = new Option("f", "final run");

		options.addOption(roundOpt);
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
