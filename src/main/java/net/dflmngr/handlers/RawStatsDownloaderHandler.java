package net.dflmngr.handlers;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.RawPlayerStats;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.RawPlayerStatsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.RawPlayerStatsServiceImpl;

public class RawStatsDownloaderHandler {
	private LoggingUtils loggerUtils;

	boolean isExecutable;

	String defaultLogfile = "RoundProgress";
	String logfile;

	RawPlayerStatsService rawPlayerStatsService;
	GlobalsService globalsService;

	public RawStatsDownloaderHandler() {
		rawPlayerStatsService = new RawPlayerStatsServiceImpl();
		globalsService = new GlobalsServiceImpl();

		isExecutable = false;
	}

	public void configureLogging(String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.logfile = logfile;
		isExecutable = true;
	}

	public void execute(int round, String homeTeam, String awayTeam, String statsUrl, boolean includeHomeTeam, boolean includeAwayTeam, String scrapingStatus) {

		try {
			if(!isExecutable) {
				configureLogging(defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			loggerUtils.log("info", "Downloading AFL stats: round={}, homeTeam={} awayTeam={} url={}", round, homeTeam, awayTeam, statsUrl);

			List<RawPlayerStats> playerStats = downloadPlayerStats(round, homeTeam, awayTeam, statsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);
			
			if(playerStats != null) {
				loggerUtils.log("info", "Saving player stats to database");


				if(includeHomeTeam) {
					rawPlayerStatsService.removeStatsForRoundAndTeam(round, homeTeam);
				}
				if(includeAwayTeam) {
					rawPlayerStatsService.removeStatsForRoundAndTeam(round, awayTeam);
				}
				rawPlayerStatsService.insertAll(playerStats, false);

				loggerUtils.log("info", "Player stats saved");
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

	private List<RawPlayerStats> downloadPlayerStats(int round, String homeTeam, String awayTeam, String statsUrl, 
	                             boolean includeHomeTeam, boolean includeAwayTeam, String scrapingStatus) {
		List<RawPlayerStats> playerStats = null;
		boolean statsDownloaded = false;

		for(int i = 0; i < 5; i++) {
			loggerUtils.log("info", "Attempt {}", i);
			try {
				RawStatsHtmlHandler htmlHandler = new RawStatsHtmlHandler();
				htmlHandler.configureLogging(logfile);
				playerStats = htmlHandler.execute(round, homeTeam, awayTeam, statsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);

				loggerUtils.log("info", "Player stats count: {}", playerStats.size());
				if(includeHomeTeam && includeAwayTeam) {
					if(playerStats.size() >= 44) {
						statsDownloaded = true;
					}
				} else {
					if(playerStats.size() >= 22) {
						statsDownloaded = true;
					}
				}

				if(statsDownloaded) {
					playerStats = null;
					break;
				}
			} catch (Exception ex) {
				loggerUtils.log("info", "Exception caught downloading stats will try again");
				loggerUtils.log("info", "Exception stacktrace={}", ExceptionUtils.getStackTrace(ex));
			}
		}

		return playerStats;
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

		RawStatsDownloaderHandler handler = new RawStatsDownloaderHandler();
		handler.configureLogging("RawPlayerDownloader");
		handler.execute(round, homeTeam, awayTeam, statsUrl, includeHomeTeam, includeAwayTeam, scrapingStatus);

		System.exit(0);
	}
}
