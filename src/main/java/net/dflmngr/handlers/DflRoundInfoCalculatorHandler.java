package net.dflmngr.handlers;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.DomainDecodes;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.DflRoundEarlyGames;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class DflRoundInfoCalculatorHandler {
	private LoggingUtils loggerUtils;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "DflRoundInfoCalculatorHandler";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	boolean isExecutable;
	
	SimpleDateFormat lockoutFormat = new SimpleDateFormat("dd/MM/yyyy h:mm a");
	
	GlobalsService globalsService;
	AflFixtureService aflFixtrureService;
	DflRoundInfoService dflRoundInfoService;
	
	String standardLockout;
	
	public DflRoundInfoCalculatorHandler() {
				
		try{			
			globalsService = new GlobalsServiceImpl();
			aflFixtrureService = new AflFixtureServiceImpl();
			dflRoundInfoService = new DflRoundInfoServiceImpl();
			
			String defaultTimezone = globalsService.getGroundTimeZone("default");
			lockoutFormat.setTimeZone(TimeZone.getTimeZone(defaultTimezone));
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute() {
		
		try {
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			standardLockout = globalsService.getStandardLockoutTime();
			//int aflRoundsMax = Integer.parseInt(globalsService.getAflRoundsMax());
			int aflRoundsMax = aflFixtrureService.getMaxAflRound();

			loggerUtils.log("info", "Executing DflRoundInfoCalculator, max AFL rounds: {}", aflRoundsMax);
			loggerUtils.log("info", "Standard round lockout time: {}", standardLockout);
			
			List<DflRoundInfo> allRoundInfo = new ArrayList<>();
			
			Map<Integer, List<AflFixture>> aflFixture = aflFixtrureService.getAflFixturneRoundBlocks();
					
			int dflRound = 1;
			
			for(int i = 1; i <= aflRoundsMax; i++) {
				
				loggerUtils.log("info", "Handling round: {}", i);
				
				List<AflFixture> aflRoundFixtures = aflFixture.get(i);
				DflRoundInfo dflRoundInfo = new DflRoundInfo();
				
				if(aflRoundFixtures.size() == 9) {
					loggerUtils.log("info", "AFL full round");
					
					dflRoundInfo.setRound(dflRound);
					dflRoundInfo.setSplitRound(DomainDecodes.DFL_ROUND_INFO.SPLIT_ROUND.NO);
					
					Map<Integer, ZonedDateTime> gameStartTimes = new HashMap<>();
					
					for(AflFixture fixture : aflRoundFixtures) {
						gameStartTimes.put(fixture.getGame(), fixture.getStartTime());
					}
					
					loggerUtils.log("info", "AFL game start times: {}", gameStartTimes);
					
					loggerUtils.log("info", "Calculating hard lockout time");
					ZonedDateTime hardLockout = calculateHardLockout(dflRound, gameStartTimes);
					dflRoundInfo.setHardLockoutTime(hardLockout);
					
					loggerUtils.log("info", "Creating round mapping: DFL rouund={}; AFL round={};", dflRound, i);
					List<DflRoundMapping> roundMappingList = new ArrayList<>();
					DflRoundMapping roundMapping = new DflRoundMapping();
					roundMapping.setRound(dflRound);
					roundMapping.setAflRound(i);
					roundMappingList.add(roundMapping);
					dflRoundInfo.setRoundMapping(roundMappingList);
					
					loggerUtils.log("info", "Calculating early games");
					List<DflRoundEarlyGames> earlyGames = calculateEarlyGames(hardLockout, aflRoundFixtures, dflRound);
					dflRoundInfo.setEarlyGames(earlyGames);
					
					allRoundInfo.add(dflRoundInfo);
				} else {
					loggerUtils.log("info", "AFL split/bye round");
					
					int gamesCount = aflRoundFixtures.size();
					loggerUtils.log("info", "Games in AFL round: {}", gamesCount);
					
					Map<Integer, List<AflFixture>> gamesInSplitRound = new HashMap<>();
					gamesInSplitRound.put(i, aflRoundFixtures);
					
					for(i++; i <= aflRoundsMax; i++) {
						List<AflFixture> aflNextRoundFixtures = aflFixture.get(i);
						gamesInSplitRound.put(i, aflNextRoundFixtures);
						
						loggerUtils.log("info", "Games in next AFL round: {}", aflNextRoundFixtures.size());
						gamesCount = gamesCount + aflNextRoundFixtures.size();
						
						if(gamesCount % 9 == 0)  {
							loggerUtils.log("info", "Found enough games to make DFL rounds ... calculation split round");
							allRoundInfo.addAll(calculateSplitRound(dflRound, gamesInSplitRound));
							for(DflRoundInfo roundInfo : allRoundInfo) {
								if(roundInfo.getRound() > dflRound) {
									dflRound = roundInfo.getRound();
								}
							}
							break;
						} 
					}	
				}
				dflRound++;
			}
			
			dflRoundInfoService.replaceAll(allRoundInfo);
			
			loggerUtils.log("info", "DflRoundInfoCalculator Complete");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private List<DflRoundInfo> calculateSplitRound(int dflRound, Map<Integer, List<AflFixture>> gamesInSplitRound) throws Exception {
		
		List<DflRoundInfo> splitRoundInfo = new ArrayList<>();
		
		Set<Integer> aflSplitRounds = gamesInSplitRound.keySet();
		int aflFirstSplitRound = Collections.min(aflSplitRounds);
		
		loggerUtils.log("info", "AFL rounds used in split round: {}", aflSplitRounds);
		
		DflRoundInfo dflRoundInfo = new DflRoundInfo();
		dflRoundInfo.setRound(dflRound);
		dflRoundInfo.setSplitRound(DomainDecodes.DFL_ROUND_INFO.SPLIT_ROUND.YES);
		
		List<DflRoundMapping> roundMappings = new ArrayList<>();
		List<AflFixture> mappedAflFixtures = new ArrayList<>();
		
		Map<Integer, ZonedDateTime> gameStartTimes = new HashMap<>();
		
		Map<String, AflFixture> workingWithGames = new HashMap<>();
		
		for(int i = 0; i < aflSplitRounds.size(); i++) {
			
			int currentSplitRound = aflFirstSplitRound + i;
			
			loggerUtils.log("info", "Working with AFL round: {}", currentSplitRound);
			
			List<AflFixture> splitRoundGames = gamesInSplitRound.get(currentSplitRound);
			for(AflFixture splitRoundGame : splitRoundGames) {
				String homeKey = splitRoundGame.getRound() + "-" + splitRoundGame.getGame() + "-1";
				String awayKey = splitRoundGame.getRound() + "-" + splitRoundGame.getGame() + "-2";
				
				if(!workingWithGames.containsKey(homeKey)) {
					loggerUtils.log("info", "Adding working with AFL game: {}", splitRoundGame);
					workingWithGames.put(homeKey, splitRoundGame);
				}
				if(!workingWithGames.containsKey(awayKey)) {
					loggerUtils.log("info", "Adding working with AFL game: {}", splitRoundGame);
					workingWithGames.put(awayKey, splitRoundGame);
				}
			}
				
			if(roundMappings.size() < 18) {
				
				loggerUtils.log("info", "Find teams to fill out round");
				
				SortedSet<String> keys = new TreeSet<>(workingWithGames.keySet());
				for(String key : keys) {
					AflFixture aflFixture = workingWithGames.get(key);
					
					String[] homeOrAway = key.split("-");
					String aflTeam = "";
					
					if(homeOrAway[2].equals("1")) {
						aflTeam = aflFixture.getHomeTeam();
					} else {
						aflTeam = aflFixture.getAwayTeam();
					}
					
					
					boolean teamAlreadyMapped = false;
					
					for(DflRoundMapping roundMap : roundMappings) {
						if(aflTeam.equals(roundMap.getAflTeam())) {
							loggerUtils.log("info", "Team: {} already mapped in round: {}", aflTeam, dflRound);
							teamAlreadyMapped = true;
							break;
						}
					}
					
					if(!teamAlreadyMapped) {
					
						DflRoundMapping roundMapping = new DflRoundMapping();
						
						roundMapping.setRound(dflRound);
						roundMapping.setAflRound(currentSplitRound);
						roundMapping.setAflRound(aflFixture.getRound());
						roundMapping.setAflGame(aflFixture.getGame());					
						roundMapping.setAflTeam(aflTeam);
						
						
						loggerUtils.log("info", "Round mapping created: {}", roundMapping);
						
						Integer gameStartTimeKey = Integer.parseInt(Integer.toString(aflFixture.getRound()) + Integer.toString(aflFixture.getGame()));
						if(!gameStartTimes.containsKey(gameStartTimeKey)) {
							gameStartTimes.put(gameStartTimeKey, aflFixture.getStartTime());
						}
						
						roundMappings.add(roundMapping);
						
						if(!mappedAflFixtures.contains(aflFixture)) {
							mappedAflFixtures.add(aflFixture);
						}
						
						workingWithGames.remove(key);
						
						if(roundMappings.size() == 18) {
							loggerUtils.log("info", "Round {} fully mapped, reminder teams: {}", dflRound, workingWithGames);
							dflRoundInfo.setRoundMapping(roundMappings);
														
							loggerUtils.log("info", "Calculating hard lockout time");
							ZonedDateTime hardLockout = calculateHardLockout(dflRound, gameStartTimes);
							dflRoundInfo.setHardLockoutTime(hardLockout);
							
							loggerUtils.log("info", "Calculating early games");
							List<DflRoundEarlyGames> earlyGames = calculateEarlyGames(hardLockout, mappedAflFixtures, dflRound);
							dflRoundInfo.setEarlyGames(earlyGames);
							
							splitRoundInfo.add(dflRoundInfo);
							
							dflRound++;
							dflRoundInfo = new DflRoundInfo();
							dflRoundInfo.setRound(dflRound);
							dflRoundInfo.setSplitRound(DomainDecodes.DFL_ROUND_INFO.SPLIT_ROUND.YES);
							roundMappings = new ArrayList<>();
							mappedAflFixtures = new ArrayList<>();
							gameStartTimes = new HashMap<>();
						}
					}
				}
				
			}	
		}
		
		return splitRoundInfo;
	}

	private ZonedDateTime calculateHardLockout(int dflRound, Map<Integer, ZonedDateTime> gameStartTimes) throws Exception {
		
		ZonedDateTime hardLockoutTime = null;
		
		loggerUtils.log("info", "Calculating hard lockout for round={}; from start times: {};", dflRound, gameStartTimes);
		
		String standardLockoutDay = (standardLockout.split(";"))[0];
		int standardLockoutHour = Integer.parseInt((standardLockout.split(";"))[1]);
		int standardLockoutMinute = Integer.parseInt((standardLockout.split(";"))[2]);
		String standardLockoutHourAMPM = (standardLockout.split(";"))[3];
		
		if(standardLockoutHourAMPM.equals("PM")) {
			standardLockoutHour = standardLockoutHour + 12;
		}
		
		loggerUtils.log("info", "Standard lockout details: day={}; hour={}; min={}; AMPM={};", standardLockoutDay, standardLockoutHour, standardLockoutMinute, standardLockoutHourAMPM);
		
		int lastGame = Collections.max(gameStartTimes.keySet());
		ZonedDateTime lastGameTime = gameStartTimes.get(lastGame);
		
		DayOfWeek lastGameDay = lastGameTime.getDayOfWeek();
		
		loggerUtils.log("info", "Last day for round: {}", lastGameDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
		
		int lastGameDayBaseWed = (lastGameDay.getValue() + DayOfWeek.WEDNESDAY.getValue()) % 7;
		int standardLockoutDayBaseWed = (DayOfWeek.valueOf(standardLockoutDay.toUpperCase()).getValue() + DayOfWeek.WEDNESDAY.getValue()) % 7;
		
		if((lastGameDayBaseWed < standardLockoutDayBaseWed) || (lastGameDay == DayOfWeek.TUESDAY) || (lastGameDay == DayOfWeek.WEDNESDAY)) {
			loggerUtils.log("info", "Last game of round is late in week, get custome lockout time");
			
			String nonStandardLockoutStr = globalsService.getNonStandardLockout(dflRound);
			
			if(nonStandardLockoutStr != null && !nonStandardLockoutStr.equals("")) {
				hardLockoutTime = LocalDateTime.parse(nonStandardLockoutStr, DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneId.of(globalsService.getGroundTimeZone("default")));
				loggerUtils.log("info", "Custom lockout time: {}", hardLockoutTime);
			} else {
				throw new Exception();
			}
		} else {
		
			int lockoutOffset = standardLockoutDayBaseWed - lastGameDayBaseWed;

			hardLockoutTime = lastGameTime.plusDays(lockoutOffset).withHour(standardLockoutHour).withMinute(standardLockoutMinute);
			
			loggerUtils.log("info", "Lockout time: {}", hardLockoutTime);
		}
			
		return hardLockoutTime;
	}
	
	private List<DflRoundEarlyGames> calculateEarlyGames(ZonedDateTime hardLockout, List<AflFixture> gamesInRound, int dflRound) throws Exception {
		
		List<DflRoundEarlyGames> earlyGames = new ArrayList<>();
		
		for(AflFixture game : gamesInRound) {
			
			if(game.getStartTime().isBefore(hardLockout)) {
				loggerUtils.log("info", "Adding early game");
				
				DflRoundEarlyGames earlyGame = new DflRoundEarlyGames();
				earlyGame.setRound(dflRound);
				earlyGame.setAflRound(game.getRound());
				earlyGame.setAflGame(game.getGame());
				earlyGame.setStartTime(game.getStartTime());
				
				earlyGames.add(earlyGame);
			}
		}
		
		return earlyGames;
	}
	
	// For internal testing
	public static void main(String[] args) {		
		DflRoundInfoCalculatorHandler testing = new DflRoundInfoCalculatorHandler();
		testing.execute();
	}
}
