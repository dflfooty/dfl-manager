package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.dflmngr.model.entity.RawPlayerStats;
import net.dflmngr.model.entity.StatsRoundPlayerStats;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.RawPlayerStatsService;
import net.dflmngr.model.service.StatsRoundPlayerStatsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.RawPlayerStatsServiceImpl;
import net.dflmngr.model.service.impl.StatsRoundPlayerStatsServiceImpl;

public class RawPlayerStatsHandler {
	private LoggingUtils loggerUtils;

	DflRoundInfoService dflRoundInfoService;
	AflFixtureService aflFixtureService;
	GlobalsService globalsService;
	StatsRoundPlayerStatsService statsRoundPlayerStatsService;
	RawPlayerStatsService rawPlayerStatsService;

	boolean isExecutable;
	String defaultLogfile = "RawPlayerStatsHandler";
	String logfile;

	public RawPlayerStatsHandler() {
		dflRoundInfoService = new DflRoundInfoServiceImpl();
		aflFixtureService = new AflFixtureServiceImpl();
		globalsService = new GlobalsServiceImpl();
		statsRoundPlayerStatsService = new StatsRoundPlayerStatsServiceImpl();
		rawPlayerStatsService = new RawPlayerStatsServiceImpl();
		
		isExecutable = false;
	}

	public void configureLogging(String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.logfile = logfile;
		isExecutable = true;
	}

	public void execute(int round, boolean scrapeAll) {

		try {
			if(!isExecutable) {
				configureLogging(defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			loggerUtils.log("info", "Downloading player stats for DFL round: {}", round);

			boolean splitDflRounds = globalsService.getSplitDflRounds();

			if(splitDflRounds) {
				loggerUtils.log("info", "Handling raw player stats with split rounds");
				handleWithSplitRounds(round, scrapeAll);
			} else {
				loggerUtils.log("info", "Handling raw player stats with stat rounds");
				handleWithStatRounds(round, scrapeAll);
			}

			dflRoundInfoService.close();
			aflFixtureService.close();
			globalsService.close();
			statsRoundPlayerStatsService.close();
			rawPlayerStatsService.close();

			loggerUtils.log("info", "Player stats downaloded");

		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}

	private void handleWithSplitRounds(int round, boolean scrapeAll) {
		DflRoundInfo dflRoundInfo = dflRoundInfoService.get(round);

		List<AflFixture> fixturesToProcess = new ArrayList<>();
		Map<String, Integer> teamsToProcess = new HashMap<>();

		loggerUtils.log("info", "Checking for AFL rounds to download");

		for(DflRoundMapping roundMapping : dflRoundInfo.getRoundMapping()) {
			int aflRound = roundMapping.getAflRound();

			loggerUtils.log("info", "DFL round includes AFL round={}", aflRound);
			if(roundMapping.getAflGame() == 0) {
				fixturesToProcess.addAll(fixturesForNoRoundMapping(aflRound, scrapeAll));
			} else {
				int aflGame = roundMapping.getAflGame();
				String team = roundMapping.getAflTeam();

				teamsToProcess.put(team, aflRound);

				AflFixture fixture = fixtureForRoundMapping(aflRound, aflGame, scrapeAll);
				if(!fixturesToProcess.contains(fixture)) {
					fixturesToProcess.add(fixture);
				}
			}
		}

		processAndUpdateFixtures(round, fixturesToProcess, teamsToProcess, scrapeAll);
	}

	private void handleWithStatRounds(int round, boolean scrapeAll) {
		DflRoundInfo dflRoundInfo = dflRoundInfoService.get(round);

		List<AflFixture> fixturesToProcess = new ArrayList<>();
		Map<String, Integer> teamsToProcess = new HashMap<>();

		loggerUtils.log("info", "Checking for AFL rounds to download");

		for(DflRoundMapping roundMapping : dflRoundInfo.getRoundMapping()) {
			int aflRound = roundMapping.getAflRound();

			loggerUtils.log("info", "DFL round includes AFL round={}", aflRound);
			if(roundMapping.getAflGame() == 0) {
				fixturesToProcess.addAll(fixturesForNoRoundMapping(aflRound, scrapeAll));
			} else {
				List<Integer> statRounds = globalsService.getStatRounds();

				if(statRounds.contains(roundMapping.getAflRound())) {
					copyStatsRoundToRawPlayerStats(round, roundMapping.getAflRound(), roundMapping.getAflTeam());
				} else {
					int aflGame = roundMapping.getAflGame();
					String team = roundMapping.getAflTeam();
	
					teamsToProcess.put(team, aflRound);

					AflFixture fixture = fixtureForRoundMapping(aflRound, aflGame, scrapeAll);
					if(!fixturesToProcess.contains(fixture)) {
						fixturesToProcess.add(fixture);
					}
				}
			}
		}

		processAndUpdateFixtures(round, fixturesToProcess, teamsToProcess, scrapeAll);
	}

	private List<AflFixture> fixturesForNoRoundMapping(int aflRound, boolean scrapeAll) {
		List<AflFixture> fixturesToProcess = new ArrayList<>();

		if(scrapeAll) {
			List<AflFixture> fixtures = aflFixtureService.getAflFixturesPlayedForRound(aflRound);
			fixturesToProcess.addAll(fixtures);
		} else {
			fixturesToProcess.addAll(aflFixtureService.getFixturesToScrape());
		}

		return fixturesToProcess;
	}

	private AflFixture fixtureForRoundMapping(int aflRound, int aflGame, boolean scrapeAll) {
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

	private void processAndUpdateFixtures(int round, List<AflFixture> fixturesToProcess, Map<String, Integer> teamsToProcess, boolean scrapeAll) {
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

			boolean includeHomeTeam = includeTeam(teamsToProcess, fixture, homeTeam);
			boolean includeAwayTeam = includeTeam(teamsToProcess, fixture, awayTeam);

			String scrapingStatus = setScrappingStatus(fixture, scrapeAll);

			loggerUtils.log("info", "Scraping status={}", scrapingStatus);

			StatsDownloaderHandler handler = new StatsDownloaderHandler(round, fullStatsUrl);
			handler.configureLogging("RawPlayerDownloader");
			handler.execute(homeTeam, awayTeam, includeHomeTeam, includeAwayTeam, scrapingStatus, false);
		}
	}

	private boolean includeTeam(Map<String, Integer> teamsToProcess, AflFixture fixture, String team) {
		boolean includeTeam = true;

		if(teamsToProcess != null && !teamsToProcess.isEmpty()) {
			int aflRoundCheck = teamsToProcess.get(team);
			if(aflRoundCheck != fixture.getRound()) {
				includeTeam = false;
			}
		}

		return includeTeam;
	}

	private String setScrappingStatus(AflFixture fixture, boolean scrapeAll) {
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
	
	private void copyStatsRoundToRawPlayerStats(int dflRound, int aflRound, String team) {
		loggerUtils.log("info", "Copying player stats from stats round to raw player stats DFL Round={}, AFL Round={}, Team={}", dflRound, aflRound, team);
		
		List<StatsRoundPlayerStats> statsRoundStats = statsRoundPlayerStatsService.getForRoundAndTeam(aflRound, team);
		List<RawPlayerStats> updatedRawPlayerStats = new ArrayList<>();

		for(StatsRoundPlayerStats playerStatRoundStats : statsRoundStats) {
			RawPlayerStats rawPlayerStats = new RawPlayerStats();

			rawPlayerStats.setRound(dflRound);
			rawPlayerStats.setName(playerStatRoundStats.getName());
			rawPlayerStats.setTeam(playerStatRoundStats.getTeam());
			rawPlayerStats.setJumperNo(playerStatRoundStats.getJumperNo());
			rawPlayerStats.setKicks(playerStatRoundStats.getKicks());
			rawPlayerStats.setHandballs(playerStatRoundStats.getHandballs());
			rawPlayerStats.setDisposals(playerStatRoundStats.getDisposals());
			rawPlayerStats.setMarks(playerStatRoundStats.getMarks());
			rawPlayerStats.setHitouts(playerStatRoundStats.getHitouts());
			rawPlayerStats.setFreesFor(playerStatRoundStats.getFreesFor());
			rawPlayerStats.setFreesAgainst(playerStatRoundStats.getFreesAgainst());
			rawPlayerStats.setTackles(playerStatRoundStats.getTackles());
			rawPlayerStats.setGoals(playerStatRoundStats.getGoals());
			rawPlayerStats.setBehinds(playerStatRoundStats.getBehinds());
			rawPlayerStats.setScrapingStatus(playerStatRoundStats.getScrapingStatus());

			updatedRawPlayerStats.add(rawPlayerStats);

			loggerUtils.log("info", "Copying player stats: statsRound={}, rawStats={}", playerStatRoundStats, rawPlayerStats);
		}

		rawPlayerStatsService.updateAll(updatedRawPlayerStats, false);
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
			testing.configureLogging("RawPlayerStatsHandlerTesting");
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
