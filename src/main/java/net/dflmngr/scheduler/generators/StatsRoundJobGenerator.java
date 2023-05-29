package net.dflmngr.scheduler.generators;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.scheduler.JobScheduler;
import net.dflmngr.utils.CronExpressionCreator;

public class StatsRoundJobGenerator {
	private LoggingUtils loggerUtils;
	
	private static String jobName = "StatsRoundPlayerStats";
	private static String jobGroup = "StatsRound";
	private static String jobClass = "net.dflmngr.scheduler.jobs.StatsRoundJob";
	
	GlobalsService globalsService;
	AflFixtureService aflFixtureService;
	
	public StatsRoundJobGenerator() {
		loggerUtils = new LoggingUtils("StatsRoundJobGenerator");
		
		try {
			globalsService = new GlobalsServiceImpl();
			aflFixtureService = new AflFixtureServiceImpl();
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}
	
	public void execute() {
		
		try {
			loggerUtils.log("info","Executing StatsRoundJobGenerator ....");

			JobScheduler.deleteGroup(jobGroup);
						
			List<Integer> statsRounds = globalsService.getStatRounds();

			for(int aflRound : statsRounds) {
				loggerUtils.log("info", "Create stats round job for AFL round={};", aflRound);
				List<AflFixture> aflFixtures = aflFixtureService.getAflFixturesForRound(aflRound);
				processFixtures(aflRound, aflFixtures);
			}
								
			loggerUtils.log("info", "StatsRoundJobGenerator completed");
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		} finally {
			globalsService.close();
			aflFixtureService.close();
		}
	}
	
	private void processFixtures(int aflRound, List<AflFixture> aflFixtures) throws Exception {
		Collections.sort(aflFixtures, Collections.reverseOrder());

		ZonedDateTime lastGameStart = aflFixtures.get(0).getStartTime();
		loggerUtils.log("info", "Last AFL game starting time={}", lastGameStart);

		createSchedule(aflRound, aflFixtures.get(0).getStartTime());
	}
			
	private void createSchedule(int aflRound, ZonedDateTime time) throws Exception {
		time = time.withHour(23);
		time = time.withMinute(0);
		scheduleJob(aflRound, time);	
	}
	
	private void scheduleJob(int round, ZonedDateTime time) throws Exception {
		loggerUtils.log("info", "Scheduling StatsRoundJob");

		CronExpressionCreator cronExpression = new CronExpressionCreator();		
		cronExpression.setTime(time.format(DateTimeFormatter.ofPattern("hh:mm a")));
		cronExpression.setStartDate(time.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
			
		Map<String, Object> jobParams = new HashMap<>();
		jobParams.put("ROUND", round);
		
		JobScheduler.schedule(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false);
	}
	
	public static void main(String[] args) {		
		StatsRoundJobGenerator testing = new StatsRoundJobGenerator();
		testing.execute();
	}

}
