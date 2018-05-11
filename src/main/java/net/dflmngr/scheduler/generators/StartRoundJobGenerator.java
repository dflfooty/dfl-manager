package net.dflmngr.scheduler.generators;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflRoundEarlyGames;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.scheduler.JobScheduler;
import net.dflmngr.utils.CronExpressionCreator;

public class StartRoundJobGenerator {
	private LoggingUtils loggerUtils;
	
	DflRoundInfoService dflRoundInfoService;
	GlobalsService globalsService;
	
	private static String jobName = "StartRoundJob";
	private static String jobGroup = "Ongoing";
	private static String jobClass = "net.dflmngr.scheduler.jobs.StartRoundJob";
	
	public StartRoundJobGenerator() {
		
		loggerUtils = new LoggingUtils("StartRoundJobGenerator");
		
		try {		
			dflRoundInfoService = new DflRoundInfoServiceImpl();
			globalsService = new GlobalsServiceImpl();
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	public void execute() {
		try {
			loggerUtils.log("infp", "Executing StartRoundJobGenerator ....");
			
			List<DflRoundInfo> dflRounds = dflRoundInfoService.findAll();
			
			for(DflRoundInfo dflRound : dflRounds) {
				loggerUtils.log("info", "Creating job entry for round={}, lockout={}", dflRound.getRound(), dflRound.getHardLockoutTime());
				createReportJobEntry(dflRound.getRound(), dflRound.getHardLockoutTime());
				
				List<DflRoundEarlyGames> earlyGames = dflRound.getEarlyGames();
				
				if(earlyGames != null && !earlyGames.isEmpty()) {
					loggerUtils.log("info", "Creating job entry for earlyGames round={}, earlyGames={}", dflRound.getRound(), earlyGames);
					createEarlyGameJobEntry(dflRound.getRound(), earlyGames);
				} else {
					loggerUtils.log("info", "No early games for round={}", dflRound.getRound());
				}
			}
			
			dflRoundInfoService.close();
			globalsService.close();
			
			loggerUtils.log("info", "StartRoundJobGenerator completed");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private void createReportJobEntry(int round, ZonedDateTime lockoutTime) throws Exception {
		
		ZonedDateTime time = lockoutTime.plusMinutes(10);
		
		CronExpressionCreator cronExpression = new CronExpressionCreator();
		cronExpression.setTime(time.format(DateTimeFormatter.ofPattern("hh:mm a")));
		cronExpression.setStartDate(time.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        
		Map<String, Object> jobParams = new HashMap<>();
		jobParams.put("ROUND", round);
		
		JobScheduler.schedule(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false);
	}
	
	private void createEarlyGameJobEntry(int round, List<DflRoundEarlyGames> earlyGames) throws Exception {
		
		Comparator<DflRoundEarlyGames> comparator = Comparator.comparingInt(DflRoundEarlyGames::getRound).thenComparingInt(DflRoundEarlyGames::getAflGame);
		earlyGames.sort(comparator);
		
		ZonedDateTime time = earlyGames.get(0).getStartTime().minusMinutes(30);
		
		CronExpressionCreator cronExpression = new CronExpressionCreator();
		cronExpression.setTime(time.format(DateTimeFormatter.ofPattern("hh:mm a")));
		cronExpression.setStartDate(time.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        
		Map<String, Object> jobParams = new HashMap<>();
		jobParams.put("ROUND", round);
	
		JobScheduler.schedule(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false);
	}
	
	public static void main(String[] args) {		
		StartRoundJobGenerator testing = new StartRoundJobGenerator();
		testing.execute();
	}
}
