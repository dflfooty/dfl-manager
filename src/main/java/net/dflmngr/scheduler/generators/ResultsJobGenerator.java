package net.dflmngr.scheduler.generators;

//import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
//import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
//import java.util.Calendar;
import java.util.Collections;
//import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import net.dflmngr.jndi.JndiProvider;
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
//import net.dflmngr.webservice.CallDflmngrWebservices;

public class ResultsJobGenerator {
	private LoggingUtils loggerUtils;
	
	private static String jobNameRoundProgress = "RoundProgress";
	private static String jobNameResults = "Results";
	private static String jobGroup = "Ongoing";
	private static String jobClass = "net.dflmngr.scheduler.jobs.ResultsJob";
	
	DflRoundInfoService dflRoundInfoService;
	AflFixtureService aflFixtureService;
	
	//private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY");
	//private static SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
	
	public ResultsJobGenerator() {
		//loggerUtils = new LoggingUtils("batch-logger", "batch.name", "ResultsJobGenerator");
		loggerUtils = new LoggingUtils("ResultsJobGenerator");
		
		try {
			//JndiProvider.bind();
			dflRoundInfoService = new DflRoundInfoServiceImpl();
			aflFixtureService = new AflFixtureServiceImpl();
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	public void execute() {
		
		try {
			loggerUtils.log("info","Executing ResultsJobGenerator ....");
			
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
				
				loggerUtils.log("info", "ResultsJobGenerator completed");
			}
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private void processFixtures(int dflRound, List<AflFixture> aflGames) throws Exception {
		
		Collections.sort(aflGames);
		
		DayOfWeek currentGameDay = null;
		DayOfWeek previousGameDay = null;
		
		ZonedDateTime gameStart = null;
		
		for(AflFixture game : aflGames) {
			
			loggerUtils.log("info", "AFL Fixture={}", game);
			
			gameStart = game.getStartTime();
			//startTimeCal = Calendar.getInstance();
			//startTimeCal.setTime(game.getStart());
			//currentGameDay = startTimeCal.get(Calendar.DAY_OF_WEEK);
			currentGameDay = gameStart.getDayOfWeek();
			
			loggerUtils.log("info", "Current Game Day={}; Previous Game Day={};", currentGameDay, previousGameDay);
			
			if(currentGameDay != previousGameDay) {
				if(currentGameDay == DayOfWeek.SUNDAY || currentGameDay == DayOfWeek.SATURDAY) {
					loggerUtils.log("info", "Creating weekend run, start time={}", gameStart);
					createWeekendSchedule(dflRound, gameStart);
				} else {
					loggerUtils.log("info", "Creating weekday run, start time={}", gameStart);
					createWeekdaySchedule(dflRound, gameStart);
				}
				
				previousGameDay = currentGameDay;
			}
			
		}
		
		loggerUtils.log("info", "Creating final run, start time={}", gameStart);
		createFinalRunSchedule(dflRound, gameStart);
	}
	
	private void createWeekendSchedule(int dflRound, ZonedDateTime time) throws Exception {
		//time.set(Calendar.HOUR_OF_DAY, 19);
		//time.set(Calendar.MINUTE, 0);
		time = time.withHour(19);
		time = time.withMinute(0);
		scheduleJob(dflRound, false, time);
			
		//time.set(Calendar.HOUR_OF_DAY, 23);
		//time.set(Calendar.MINUTE, 0);
		time = time.withHour(23);
		time = time.withMinute(0);
		scheduleJob(dflRound, false, time);	
	}
	
	private void createWeekdaySchedule(int dflRound, ZonedDateTime time) throws Exception {
		//time.set(Calendar.HOUR_OF_DAY, 23);
		//time.set(Calendar.MINUTE, 0);
		time = time.withHour(23);
		time = time.withMinute(0);
		scheduleJob(dflRound, false, time);	
	}
	
	private void createFinalRunSchedule(int dflRound, ZonedDateTime time) throws Exception {
		//time.set(Calendar.DAY_OF_MONTH, time.get(Calendar.DAY_OF_MONTH)+1);
		//time.set(Calendar.HOUR_OF_DAY, 9);
		//time.set(Calendar.MINUTE, 0);
		time = time.plusDays(1);
		time = time.withHour(9);
		time = time.withMinute(0);
		scheduleJob(dflRound, true, time);	
	}
	
	private void scheduleJob(int round, boolean isFinal, ZonedDateTime time) throws Exception {
		CronExpressionCreator cronExpression = new CronExpressionCreator();
		//cronExpression.setTime(timeFormat.format(time.getTime()));
		//cronExpression.setStartDate(dateFormat.format(time.getTime()));
		
		//ZonedDateTime timeUtc = time.withZoneSameInstant(ZoneId.of("UTC"));
		
		//cronExpression.setTime(timeUtc.format(DateTimeFormatter.ofPattern("hh:mm a")));
		//cronExpression.setStartDate(timeUtc.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
		
		cronExpression.setTime(time.format(DateTimeFormatter.ofPattern("hh:mm a")));
		cronExpression.setStartDate(time.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
		
		loggerUtils.log("info", "Cron date={}; time={};", cronExpression.getStartDate(), cronExpression.getTime());
		
		Map<String, Object> jobParams = new HashMap<>();
		jobParams.put("ROUND", round);
		jobParams.put("IS_FINAL", isFinal);
		
		String jobName = "";
		
		if(isFinal) {
			jobName = jobNameResults;
		} else {
			jobName = jobNameRoundProgress;
		}
				
		//CallDflmngrWebservices.scheduleJob(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false, loggerUtils);
		JobScheduler.schedule(jobName, jobGroup, jobClass, jobParams, cronExpression.getCronExpression(), false);
	}
	
	public static void main(String[] args) {		
		ResultsJobGenerator testing = new ResultsJobGenerator();
		testing.execute();
	}

}
