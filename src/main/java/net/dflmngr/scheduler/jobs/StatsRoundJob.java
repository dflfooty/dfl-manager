package net.dflmngr.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import net.dflmngr.handlers.StatsRoundPlayerStatsHandler;
import net.dflmngr.logging.LoggingUtils;

public class StatsRoundJob implements Job {
	private static final String ROUND = "ROUND";
		
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LoggingUtils loggerUtils = new LoggingUtils("Scheduler");
		
		try {
			loggerUtils.log("info", "StatsRoundJob starting ...");
			
			JobDataMap data = context.getJobDetail().getJobDataMap(); 
			
			int round = data.getInt(ROUND);
			
			String logFile = "StatsRoundRound_R";
			
			StatsRoundPlayerStatsHandler statsRoundHandler = new StatsRoundPlayerStatsHandler();
			statsRoundHandler.configureLogging("online.name", "online-logger", logFile);

			loggerUtils.log("info", "Running {}", logFile);
			statsRoundHandler.execute(round);
			loggerUtils.log("info", "{} completed", logFile);
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}

	}

}
