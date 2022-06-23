package net.dflmngr.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import net.dflmngr.handlers.ResultsHandler;
import net.dflmngr.logging.LoggingUtils;

public class ResultsJob implements Job {
	private LoggingUtils loggerUtils;
	
	public static final String ROUND = "ROUND";
	public static final String IS_FINAL = "IS_FINAL";
	public static final String ONGOING = "ONGOING";
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		loggerUtils = new LoggingUtils("Scheduler");
		
		try {
			loggerUtils.log("info", "RoundProgressJob starting ...");
			
			JobDataMap data = context.getJobDetail().getJobDataMap(); 
			
			int round = data.getInt(ROUND);
			boolean isFinal = data.getBoolean(IS_FINAL);
			boolean ongoing = data.getBoolean(ONGOING);
			
			String logFile = "";
			boolean sendReport = false;
			boolean skipStats = false;
			
			if(ongoing) {
				logFile = "ResultsOngoing";
			} else {			
				if(isFinal) {
					logFile = "ResultsRound_R" + round;
					sendReport = true;
				} else {
					logFile = "ProgressRound_R" + round;
				}
			}
			
			ResultsHandler resultsHandler = new ResultsHandler();
			resultsHandler.configureLogging("online.name", "online-logger", logFile);

			loggerUtils.log("info", "Running {}", logFile);
			resultsHandler.execute(round, isFinal, null, skipStats, sendReport);
			loggerUtils.log("info", "{} completed", logFile);
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}

	}

}
