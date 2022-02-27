package net.dflmngr.scheduler.generators;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.entity.keys.AflFixturePK;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.scheduler.JobScheduler;
import net.dflmngr.utils.CronExpressionCreator;
import net.dflmngr.utils.DflmngrUtils;


public class ResultsJobGenerator {
	private LoggingUtils loggerUtils;
	
	private static String jobNameRoundProgress = "RoundProgress";
	private static String jobNameResults = "Results";
	private static String jobNameOngoingResults = "OngoingResults";
	private static String jobGroup = "Results";
	private static String jobClass = "net.dflmngr.scheduler.jobs.ResultsJob";
	
	DflRoundInfoService dflRoundInfoService;
	AflFixtureService aflFixtureService;
	
	public ResultsJobGenerator() {
		loggerUtils = new LoggingUtils("ResultsJobGenerator");
		
		try {
			dflRoundInfoService = new DflRoundInfoServiceImpl();
			aflFixtureService = new AflFixtureServiceImpl();
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	public void execute() {
		
		try {
			loggerUtils.log("info","Executing ResultsJobGenerator ....");

			JobScheduler.deleteGroup(jobGroup);
			
			createOngoingSchedule();
			
			List<DflRoundInfo> dflSeason = dflRoundInfoService.findAll();
			
			for(DflRoundInfo roundInfo : dflSeason) {
				List<DflRoundMapping> roundMapping = roundInfo.getRoundMapping();
				
				List<AflFixture> dflAflGames = new ArrayList<>();
				for(DflRoundMapping mapping : roundMapping) {
					loggerUtils.log("info", "Finding AFL games for: DFL round={}; AFL round={};", roundInfo.getRound(), mapping.getAflRound());
					if(mapping.getAflGame() == 0) {
						loggerUtils.log("info", "No AFL game mapping adding all games");
						dflAflGames.addAll(aflFixtureService.getAflFixturesForRound(mapping.getAflRound()));
					} else {
						loggerUtils.log("info", "Bye round adding: AFL round={}; game={};", mapping.getAflRound(), mapping.getAflGame());
						AflFixturePK aflFixturePK = new AflFixturePK();
						aflFixturePK.setRound(mapping.getAflRound());
						aflFixturePK.setGame(mapping.getAflGame());
						dflAflGames.add(aflFixtureService.get(aflFixturePK));
					}
				}
				
				loggerUtils.log("info", "Processing fixtures: DFL round={}; fixtures={}", roundInfo.getRound(), dflAflGames);
				processFixtures(roundInfo.getRound(), dflAflGames);
			}
						
			loggerUtils.log("info", "ResultsJobGenerator completed");
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		} finally {
			dflRoundInfoService.close();
			aflFixtureService.close();
		}
	}
	
	private void processFixtures(int dflRound, List<AflFixture> aflGames) throws Exception {
		
		Collections.sort(aflGames, Collections.reverseOrder());
		
		DayOfWeek currentGameDay = null;
		DayOfWeek previousGameDay = null;
		
		ZonedDateTime gameStart = null;
		ZonedDateTime lastGameStart = null;
		
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
		
		for(AflFixture game : aflGames) {
			
			loggerUtils.log("info", "AFL Fixture={}", game);
			
			gameStart = game.getStartTime();
			currentGameDay = gameStart.getDayOfWeek();
			
			loggerUtils.log("info", "Current Game Day={}; Previous Game Day={};", currentGameDay, previousGameDay);
			
			if(currentGameDay != previousGameDay && gameStart.isAfter(now)) {
				boolean lastGameDay = false;
				if(previousGameDay == null) {
					lastGameStart = gameStart;
					lastGameDay = true;
				}
				if(currentGameDay == DayOfWeek.SUNDAY || currentGameDay == DayOfWeek.SATURDAY) {
					loggerUtils.log("info", "Creating weekend run, start time={}; lastGameDay={}", gameStart, lastGameDay);
					createWeekendSchedule(dflRound, gameStart, lastGameDay);
				} else {
					loggerUtils.log("info", "Creating weekday run, start time={}; lastGameDay={}", gameStart, lastGameDay);
					createWeekdaySchedule(dflRound, gameStart, lastGameDay);
				}
				
				previousGameDay = currentGameDay;
			}
			
		}
		
		loggerUtils.log("info", "Creating final run, start time={}", lastGameStart);
		if(lastGameStart != null) {
			createFinalRunSchedule(dflRound, lastGameStart);
		}
	}
	
	private void createOngoingSchedule() throws Exception {
		scheduleJob(0, true, false, null);	
	}
	
	private void createWeekendSchedule(int dflRound, ZonedDateTime time, boolean lastGameDay) throws Exception {
		time = time.withHour(19);
		time = time.withMinute(0);
		scheduleJob(dflRound, false, false, time);
		
		if(!lastGameDay) {
			time = time.withHour(23);
			time = time.withMinute(0);
			scheduleJob(dflRound, false, false, time);
		}
	}
	
	private void createWeekdaySchedule(int dflRound, ZonedDateTime time, boolean lastGameDay) throws Exception {
		if(!lastGameDay) {
			time = time.withHour(23);
			time = time.withMinute(0);
			scheduleJob(dflRound, false, false, time);
		}
	}
	
	private void createFinalRunSchedule(int dflRound, ZonedDateTime time) throws Exception {
		time = time.withHour(23);
		time = time.withMinute(0);
		scheduleJob(dflRound, false, true, time);	
	}
	
	private void scheduleJob(int round, boolean ongoing, boolean isFinal, ZonedDateTime time) throws Exception {
		
		if(ongoing) {
			loggerUtils.log("info", "Scheduling ongoing ResultsJob");
			
			Map<String, Object> jobParams = new HashMap<>();
			jobParams.put("ROUND", round);
			jobParams.put("IS_FINAL", isFinal);
			jobParams.put("ONGOING", ongoing);
			
			String jobName = jobNameOngoingResults;
			JobScheduler.schedule(jobName, jobGroup, jobClass, jobParams, "0 3/5 * 1/1 * ? *", false);
		} else {
			loggerUtils.log("info", "Scheduling fixed ResultsJob, isFinal={}", isFinal);
			
			CronExpressionCreator cronExpression = new CronExpressionCreator();		
			cronExpression.setTime(time.format(DateTimeFormatter.ofPattern("hh:mm a")));
			cronExpression.setStartDate(time.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
			
			loggerUtils.log("info", "Cron date={}; time={};", cronExpression.getStartDate(), cronExpression.getTime());
			
			Map<String, Object> jobParams = new HashMap<>();
			jobParams.put("ROUND", round);
			jobParams.put("IS_FINAL", isFinal);
			jobParams.put("ONGOING", ongoing);
			
			String jobName = "";
			
			if(isFinal) {
				jobName = jobNameResults;
			} else {
				jobName = jobNameRoundProgress;
			}
					
			JobScheduler.schedule(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false);
		}
	}
	
	public static void main(String[] args) {		
		ResultsJobGenerator testing = new ResultsJobGenerator();
		testing.execute();
	}

}
