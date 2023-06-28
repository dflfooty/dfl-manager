package net.dflmngr.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import net.dflmngr.handlers.ResultsHandler;
import net.dflmngr.logging.LoggingUtils;

public class ResultsJob implements Job {	
	private static final String ROUND = "ROUND";
	private static final String IS_FINAL = "IS_FINAL";
	private static final String ONGOING = "ONGOING";
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LoggingUtils loggerUtils = new LoggingUtils("Scheduler");
		
		try {
			loggerUtils.log("info", "RoundResultsJob starting ...");
			
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
			resultsHandler.configureLogging(logFile);

			loggerUtils.log("info", "Running {}", logFile);
			resultsHandler.execute(round, isFinal, null, skipStats, sendReport);
			loggerUtils.log("info", "{} completed", logFile);
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}

	}

}
