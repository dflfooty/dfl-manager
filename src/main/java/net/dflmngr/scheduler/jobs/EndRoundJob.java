package net.dflmngr.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import net.dflmngr.handlers.EndRoundHandler;
import net.dflmngr.logging.LoggingUtils;

public class EndRoundJob implements Job {
	private LoggingUtils loggerUtils;
	
	public static String ROUND = "ROUND";
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		loggerUtils = new LoggingUtils("Scheduler");
		
		try {
			loggerUtils.log("info", "EndRoundJob starting ...");
			
			JobDataMap data = context.getJobDetail().getJobDataMap(); 
			
			int round = data.getInt(ROUND);
			
			EndRoundHandler endRound = new EndRoundHandler();
			endRound.configureLogging("online.name", "online-logger", ("EndRound_R"+round));

			loggerUtils.log("info", "Running EndRound: round={};", round);
			endRound.execute(round, null);
			loggerUtils.log("info", "EndRoundJob completed");
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}
}
