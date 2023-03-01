package net.dflmngr.handlers;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.RawPlayerStats;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.RawPlayerStatsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.RawPlayerStatsServiceImpl;

public class StatsDownloaderHandler {
	private LoggingUtils loggerUtils;

	boolean isExecutable;

	String defaultLogfile = "RoundProgress";
	String logfile;

	RawPlayerStatsService rawPlayerStatsService;
	GlobalsService globalsService;

	int round;
	String statsUrl;

	public StatsDownloaderHandler(int round, String statsUrl) {
		rawPlayerStatsService = new RawPlayerStatsServiceImpl();
		globalsService = new GlobalsServiceImpl();

		isExecutable = false;

		this.round = round;
		this.statsUrl = statsUrl;
	}

	public void configureLogging(String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.logfile = logfile;
		isExecutable = true;
	}

	public void execute(String homeTeam, String awayTeam, boolean includeHomeTeam, boolean includeAwayTeam, String scrapingStatus, boolean isStatsRound) {

		try {
			if(!isExecutable) {
				configureLogging(defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			if(isStatsRound) {
				loggerUtils.log("info", "Running for Stats round: AFL round={}", round);
			}

			loggerUtils.log("info", "Downloading AFL stats: round={}, homeTeam={} awayTeam={} url={}", round, homeTeam, awayTeam, statsUrl);

			List<RawPlayerStats> playerStats = null;
			boolean statsDownloaded = false;
			for(int i = 0; i < 5; i++) {
				loggerUtils.log("info", "Attempt {}", i);
				try {
					StatsHtmlHandler htmlHandler = new StatsHtmlHandler();
					htmlHandler.configureLogging(logfile);

					playerStats = htmlHandler.execute(round, homeTeam, awayTeam, statsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);

					loggerUtils.log("info", "Player stats count: {}", playerStats.size());
					if(includeHomeTeam && includeAwayTeam) {
						if(playerStats.size() >= 44) {
							statsDownloaded = true;
							break;
						}
					} else {
						if(playerStats.size() >= 22) {
							statsDownloaded = true;
							break;
						}
					}
				} catch (Exception ex) {
					loggerUtils.log("info", "Exception caught downloading stats will try again");
					loggerUtils.log("info", "Exception stacktrace={}", ExceptionUtils.getStackTrace(ex));
				}
			}
			if(statsDownloaded) {
				loggerUtils.log("info", "Saving player stats to database");

				if(isStatsRound) {

				} else {
					if(includeHomeTeam) {
						rawPlayerStatsService.removeStatsForRoundAndTeam(round, homeTeam);
					}
					if(includeAwayTeam) {
						rawPlayerStatsService.removeStatsForRoundAndTeam(round, awayTeam);
					}
					rawPlayerStatsService.insertAll(playerStats, false);

					loggerUtils.log("info", "Player stats saved");
				}
			} else {
				loggerUtils.log("info", "Player stats were not downloaded");
			}
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		} finally {
			rawPlayerStatsService.close();
			globalsService.close();
		}
	}

	// For internal testing
	public static void main(String[] args) {

		int round = Integer.parseInt(args[0]);
		String homeTeam = args[1];
		String awayTeam = args[2];
		String statsUrl = args[3];
		boolean includeHomeTeam = Boolean.parseBoolean(args[4]);
		boolean includeAwayTeam = Boolean.parseBoolean(args[5]);
		String scrapingStatus = args[6];
		boolean isStatsRound = Boolean.parseBoolean(args[6]);

		StatsDownloaderHandler handler = new StatsDownloaderHandler(round, statsUrl);
		handler.configureLogging("RawPlayerDownloader");
		handler.execute(homeTeam, awayTeam, includeHomeTeam, includeAwayTeam, scrapingStatus, isStatsRound);

		System.exit(0);
	}
}
