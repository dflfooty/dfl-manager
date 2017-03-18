package net.dflmngr.scheduler.generators;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.scheduler.JobScheduler;
import net.dflmngr.utils.CronExpressionCreator;

public class EndRoundJobGenerator {
	private LoggingUtils loggerUtils;
	
	DflRoundInfoService dflRoundInfoService;
	GlobalsService globalsService;
	
	private static String jobName = "EndRoundJob";
	private static String jobGroup = "Ongoing";
	private static String jobClass = "net.dflmngr.scheduler.jobs.EndRoundJob";
	
	public EndRoundJobGenerator() {
		loggerUtils = new LoggingUtils("EndRoundJobGenerator");
		
		try {		
			dflRoundInfoService = new DflRoundInfoServiceImpl();
			globalsService = new GlobalsServiceImpl();
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	public void execute() {
		try {
			loggerUtils.log("infp", "Executing EndRoundJobGenerator ....");
			
			List<DflRoundInfo> dflRounds = dflRoundInfoService.findAll();
			
			for(DflRoundInfo dflRound : dflRounds) {
				loggerUtils.log("info", "Creating job entry for round={}, lockout={}", dflRound.getRound(), dflRound.getHardLockoutTime());
				createReportJobEntry(dflRound.getRound(), dflRound.getHardLockoutTime());
								
				loggerUtils.log("info", "EndRoundJobGenerator completed");
			}
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private void createReportJobEntry(int round, ZonedDateTime time) throws Exception {
		
		CronExpressionCreator cronExpression = new CronExpressionCreator();
		
		ZonedDateTime runTime = time.withZoneSameInstant(ZoneId.of("UTC")).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
		runTime = runTime.withHour(16).withMinute(0);
		
		cronExpression.setTime(runTime.format(DateTimeFormatter.ofPattern("hh:mm a")));
		cronExpression.setStartDate(runTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        
		Map<String, Object> jobParams = new HashMap<>();
		jobParams.put("ROUND", round);
		
		//CallDflmngrWebservices.scheduleJob(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false, loggerUtils);
		JobScheduler.schedule(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false);
	}
	
	public static void main(String[] args) {		
		StartRoundJobGenerator testing = new StartRoundJobGenerator();
		testing.execute();
	}
}