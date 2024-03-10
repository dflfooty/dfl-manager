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

import net.dflmngr.exceptions.MissingNonStandardLockoutException;
import net.dflmngr.exceptions.StatsRoundMissingTeamException;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.DomainDecodes;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.AflTeam;
import net.dflmngr.model.entity.DflRoundEarlyGames;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.AflTeamService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.AflTeamServiceImpl;
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
	AflFixtureService aflFixtureService;
	DflRoundInfoService dflRoundInfoService;
	AflTeamService aflTeamService;
	
	String standardLockout;

	List<String> statsRoundTracking;
	
	public DflRoundInfoCalculatorHandler() {
				
		try{			
			globalsService = new GlobalsServiceImpl();
			aflFixtureService = new AflFixtureServiceImpl();
			dflRoundInfoService = new DflRoundInfoServiceImpl();
			aflTeamService = new AflTeamServiceImpl();
			
			String defaultTimezone = globalsService.getGroundTimeZone("default");
			lockoutFormat.setTimeZone(TimeZone.getTimeZone(defaultTimezone));

			configureStatsRoundTracking();
			
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
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
			int aflRoundsMax = aflFixtureService.getMaxAflRound();

			loggerUtils.log("info", "Executing DflRoundInfoCalculator, max AFL rounds: {}", aflRoundsMax);
			loggerUtils.log("info", "Standard round lockout time: {}", standardLockout);
			
			Map<Integer, List<AflFixture>> aflFixtures = aflFixtureService.getAflFixturneRoundBlocks();

			List<DflRoundInfo> allRoundInfo;

			if(globalsService.getSplitDflRounds()) {
				loggerUtils.log("info", "Creating DFL round info with split rounds");
				allRoundInfo = createWithSplitRounds(aflRoundsMax, aflFixtures);
			} else {
				loggerUtils.log("info", "Creating DFL round info with stat rounds");
				allRoundInfo = createWithStatsRounds(aflRoundsMax, aflFixtures);
			}
			
			dflRoundInfoService.replaceAll(allRoundInfo);
			
			loggerUtils.log("info", "DflRoundInfoCalculator Complete");
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}

	private void configureStatsRoundTracking() {
		statsRoundTracking = new ArrayList<>();
		List<Integer> aflStatsRounds = globalsService.getStatRounds();

		for(int aflStatRound : aflStatsRounds) {
			List<AflFixture> aflFixtures = aflFixtureService.getAflFixturesForRound(aflStatRound);
			for(AflFixture aflFixture : aflFixtures) {
				statsRoundTracking.add(aflStatRound + "-" + aflFixture.getHomeTeam());
				statsRoundTracking.add(aflStatRound + "-" + aflFixture.getAwayTeam());
			}
		}

		Collections.sort(statsRoundTracking);
	}

	private List<DflRoundInfo> createWithSplitRounds(int aflRoundsMax, Map<Integer, List<AflFixture>> aflFixtures) {
		
		List<DflRoundInfo> allRoundInfo = new ArrayList<>();
		int dflRound = 1;
				
		for(int aflRound = 1; aflRound <= aflRoundsMax; aflRound++) {
			
			loggerUtils.log("info", "Handling round: {}", aflRound);
			
			List<AflFixture> aflRoundFixtures = aflFixtures.get(aflRound);
			
			if(aflRoundFixtures.size() == 9) {
				allRoundInfo.add(createFullAflRound(dflRound, aflRound, aflRoundFixtures));
			} else {
				allRoundInfo.addAll(createSplitAflRound(dflRound, aflRound, aflRoundsMax, aflFixtures));
			}
			dflRound++;
		}

		return allRoundInfo;
	}

	private List<DflRoundInfo> createWithStatsRounds(int aflRoundsMax, Map<Integer, List<AflFixture>> aflFixtures) {

		List<DflRoundInfo> allRoundInfo = new ArrayList<>();
		int dflRound = 1;
		List<Integer> aflStatsRounds = globalsService.getStatRounds();

		for(int aflRound = 1; aflRound <= aflRoundsMax; aflRound++) {
			
			loggerUtils.log("info", "Handling round: {}", aflRound);
			List<AflFixture> aflRoundFixtures = aflFixtures.get(aflRound);
			
			if(!aflStatsRounds.contains(aflRound)) {
				if(aflRoundFixtures.size() == 9) {
					allRoundInfo.add(createFullAflRound(dflRound, aflRound, aflRoundFixtures));
				} else {
					allRoundInfo.add(createStatsAflRound(dflRound, aflRound, aflRoundFixtures));
				}
				dflRound++;
			} else {
				loggerUtils.log("info", "AFL stats round, skipping");
			}

		}

		return allRoundInfo;

	}

	private DflRoundInfo createFullAflRound(int dflRound, int aflRound, List<AflFixture> aflRoundFixtures) {

		loggerUtils.log("info", "AFL full round");

		DflRoundInfo dflRoundInfo = new DflRoundInfo();	
		dflRoundInfo.setRound(dflRound);
		dflRoundInfo.setSplitRound(DomainDecodes.DFL_ROUND_INFO.SPLIT_ROUND.NO);
		
		Map<Integer, ZonedDateTime> gameStartTimes = new HashMap<>();
		
		for(AflFixture fixture : aflRoundFixtures) {
			gameStartTimes.put(fixture.getGame(), fixture.getStartTime());
		}
		
		loggerUtils.log("info", "AFL game start times: {}", gameStartTimes);
		
		ZonedDateTime hardLockout = calculateHardLockout(dflRound, gameStartTimes);
		dflRoundInfo.setHardLockoutTime(hardLockout);
		
		loggerUtils.log("info", "Creating round mapping: DFL rouund={}; AFL round={};", dflRound, aflRound);
		List<DflRoundMapping> roundMappingList = new ArrayList<>();
		DflRoundMapping roundMapping = new DflRoundMapping();
		roundMapping.setRound(dflRound);
		roundMapping.setAflRound(aflRound);
		roundMappingList.add(roundMapping);
		dflRoundInfo.setRoundMapping(roundMappingList);
		
		List<DflRoundEarlyGames> earlyGames = calculateEarlyGames(hardLockout, aflRoundFixtures, dflRound);
		dflRoundInfo.setEarlyGames(earlyGames);
		
		return dflRoundInfo;
	}

	private List<DflRoundInfo> createSplitAflRound(int dflRound, int aflRound, int aflRoundsMax, Map<Integer, List<AflFixture>> aflFixtures) {
		loggerUtils.log("info", "AFL split/bye round");
		
		List<DflRoundInfo> splitRoundsInfo = new ArrayList<>();
		List<AflFixture> aflRoundFixtures = aflFixtures.get(aflRound);

		int gamesCount = aflFixtures.size();
		loggerUtils.log("info", "Games in AFL round: {}", gamesCount);
		
		Map<Integer, List<AflFixture>> gamesInSplitRound = new HashMap<>();
		gamesInSplitRound.put(aflRound, aflRoundFixtures);
		
		for(aflRound++; aflRound <= aflRoundsMax; aflRound++) {
			List<AflFixture> aflNextRoundFixtures = aflFixtures.get(aflRound);
			gamesInSplitRound.put(aflRound, aflNextRoundFixtures);
			
			loggerUtils.log("info", "Games in next AFL round: {}", aflNextRoundFixtures.size());
			gamesCount = gamesCount + aflNextRoundFixtures.size();
			
			if(gamesCount % 9 == 0)  {
				loggerUtils.log("info", "Found enough games to make DFL rounds ... calculation split round");
				splitRoundsInfo.addAll(calculateSplitRound(dflRound, gamesInSplitRound));
				break;
			} 
		}
		
		return splitRoundsInfo;
	}

	private DflRoundInfo createStatsAflRound(int dflRound, int aflRound, List<AflFixture> aflRoundFixtures) {
		loggerUtils.log("info", "AFL bye round with stats");

		DflRoundInfo dflRoundInfo = new DflRoundInfo();
		dflRoundInfo.setRound(dflRound);
		dflRoundInfo.setSplitRound(DomainDecodes.DFL_ROUND_INFO.SPLIT_ROUND.YES);

		List<DflRoundMapping> roundMappings = new ArrayList<>();
		List<String> aflTeamsInRound = new ArrayList<>();
		List<String> aflTeamsNotInRound = new ArrayList<>();

		Map<Integer, ZonedDateTime> gameStartTimes = new HashMap<>();
		
		for(AflFixture fixture : aflRoundFixtures) {
			gameStartTimes.put(fixture.getGame(), fixture.getStartTime());
			aflTeamsInRound.add(fixture.getHomeTeam());
			aflTeamsInRound.add(fixture.getAwayTeam());
		}
		
		loggerUtils.log("info", "AFL game start times: {}", gameStartTimes);
		
		ZonedDateTime hardLockout = calculateHardLockout(dflRound, gameStartTimes);
		dflRoundInfo.setHardLockoutTime(hardLockout);

		List<AflTeam> allAflTeams = aflTeamService.findAll();

		for(AflTeam aflTeam : allAflTeams) {
			if(!aflTeamsInRound.contains(aflTeam.getTeamId())) {
				aflTeamsNotInRound.add(aflTeam.getTeamId());
			}
		}

		List<Integer> aflStatsRounds = globalsService.getStatRounds();

		loggerUtils.log("info", "Creating round mapping: DFL round={}; AFL round={}; AFL stats round={}", dflRound, aflRound, aflStatsRounds);

		DflRoundMapping roundMapping = new DflRoundMapping();
		roundMapping.setRound(dflRound);
		roundMapping.setAflRound(aflRound);

		roundMappings.add(roundMapping);
		
		int useAflStatsRound = -1;
		for(String aflTeamId : aflTeamsNotInRound) {
			for(int aflStatsRound : aflStatsRounds) {
				String check = aflStatsRound + "-" + aflTeamId;
				if(statsRoundTracking.contains(check)) {
					statsRoundTracking.remove(check);
					useAflStatsRound = aflStatsRound;
					break;
				}
			}

			if(useAflStatsRound == -1) {
				throw new StatsRoundMissingTeamException(aflTeamId, dflRound, aflRound);
			}

			AflFixture aflStatsFixture = aflFixtureService.getAflFixtureForRoundAndTeam(useAflStatsRound, aflTeamId);

			roundMapping = new DflRoundMapping();
			roundMapping.setRound(dflRound);
			roundMapping.setAflRound(aflStatsFixture.getRound());
			roundMapping.setAflGame(aflStatsFixture.getGame());
			roundMapping.setAflTeam(aflTeamId);

			roundMappings.add(roundMapping);
		}

		dflRoundInfo.setRoundMapping(roundMappings);

		List<DflRoundEarlyGames> earlyGames = calculateEarlyGames(hardLockout, aflRoundFixtures, dflRound);
		dflRoundInfo.setEarlyGames(earlyGames);
		
		return dflRoundInfo;		
	}
	
	private List<DflRoundInfo> calculateSplitRound(int dflRound, Map<Integer, List<AflFixture>> gamesInSplitRound) {
		
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
			
			workingWithGames.putAll(getGamesToWorkWith(currentSplitRound, gamesInSplitRound));
				
			if(roundMappings.size() < 18) {
				
				loggerUtils.log("info", "Find teams to fill out round");
				
				SortedSet<String> keys = new TreeSet<>(workingWithGames.keySet());
				for(String key : keys) {
					AflFixture aflFixture = workingWithGames.get(key);
					
					String teamToMap = isTeamMapped(key, roundMappings, aflFixture);
					
					if(!teamToMap.equals("")) {
					
						roundMappings.add(setRoundMapping(dflRound, currentSplitRound, aflFixture, teamToMap));					
												
						Integer gameStartTimeKey = Integer.parseInt(Integer.toString(aflFixture.getRound()) + Integer.toString(aflFixture.getGame()));
						gameStartTimes.computeIfAbsent(gameStartTimeKey, k -> aflFixture.getStartTime());

						if(!mappedAflFixtures.contains(aflFixture)) {
							mappedAflFixtures.add(aflFixture);
						}
						
						workingWithGames.remove(key);
						
						if(roundMappings.size() == 18) {							
							splitRoundInfo.add(populateRoundInfoForSplitRound(dflRoundInfo, roundMappings, workingWithGames, gameStartTimes, mappedAflFixtures));
							
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

	private Map<String, AflFixture> getGamesToWorkWith(int currentSplitRound, Map<Integer, List<AflFixture>> gamesInSplitRound) {
		Map<String, AflFixture> workingWithGames = new HashMap<>();

		List<AflFixture> splitRoundGames = gamesInSplitRound.get(currentSplitRound);
		for(AflFixture splitRoundGame : splitRoundGames) {
			String homeKey = splitRoundGame.getRound() + "-" + splitRoundGame.getGame() + "-1";
			String awayKey = splitRoundGame.getRound() + "-" + splitRoundGame.getGame() + "-2";
			
			workingWithGames.computeIfAbsent(homeKey, k -> {
				loggerUtils.log("info", "Adding working with AFL game: {}", splitRoundGame);
				return splitRoundGame;
			});
			workingWithGames.computeIfAbsent(awayKey, k -> {
				loggerUtils.log("info", "Adding working with AFL game: {}", splitRoundGame);
				return splitRoundGame;
			});
		}

		return workingWithGames;
	}

	private String isTeamMapped(String key, List<DflRoundMapping> roundMappings,  AflFixture aflFixture) {
		String[] homeOrAway = key.split("-");
		String aflTeam = "";
		
		if(homeOrAway[2].equals("1")) {
			aflTeam = aflFixture.getHomeTeam();
		} else {
			aflTeam = aflFixture.getAwayTeam();
		}
				
		for(DflRoundMapping roundMap : roundMappings) {
			if(aflTeam.equals(roundMap.getAflTeam())) {
				loggerUtils.log("info", "Team: {} already mapped in round: {}", aflTeam, roundMap.getRound());
				aflTeam = "";
				break;
			}
		}

		return aflTeam;
	}

	private DflRoundMapping setRoundMapping(int dflRound, int currentSplitRound, AflFixture aflFixture, String teamToMap) {
		DflRoundMapping roundMapping = new DflRoundMapping();
						
		roundMapping.setRound(dflRound);
		roundMapping.setAflRound(currentSplitRound);
		roundMapping.setAflRound(aflFixture.getRound());
		roundMapping.setAflGame(aflFixture.getGame());					
		roundMapping.setAflTeam(teamToMap);						
		
		loggerUtils.log("info", "Round mapping created: {}", roundMapping);

		return roundMapping;
	}

	private DflRoundInfo populateRoundInfoForSplitRound(DflRoundInfo dflRoundInfo, List<DflRoundMapping> roundMappings, Map<String, AflFixture> workingWithGames,
													  Map<Integer, ZonedDateTime> gameStartTimes, List<AflFixture> mappedAflFixtures) {
		loggerUtils.log("info", "Round {} fully mapped, reminder teams: {}", dflRoundInfo.getRound(), workingWithGames);
		dflRoundInfo.setRoundMapping(roundMappings);
									
		ZonedDateTime hardLockout = calculateHardLockout(dflRoundInfo.getRound(), gameStartTimes);
		dflRoundInfo.setHardLockoutTime(hardLockout);
		
		loggerUtils.log("info", "Calculating early games");
		List<DflRoundEarlyGames> earlyGames = calculateEarlyGames(hardLockout, mappedAflFixtures, dflRoundInfo.getRound());
		dflRoundInfo.setEarlyGames(earlyGames);

		return dflRoundInfo;
	}

	private ZonedDateTime calculateHardLockout(int dflRound, Map<Integer, ZonedDateTime> gameStartTimes) {
		
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
				throw new MissingNonStandardLockoutException();
			}
		} else {
		
			int lockoutOffset = standardLockoutDayBaseWed - lastGameDayBaseWed;

			hardLockoutTime = lastGameTime.plusDays(lockoutOffset).withHour(standardLockoutHour).withMinute(standardLockoutMinute);
			
			loggerUtils.log("info", "Lockout time: {}", hardLockoutTime);
		}
			
		return hardLockoutTime;
	}
	
	private List<DflRoundEarlyGames> calculateEarlyGames(ZonedDateTime hardLockout, List<AflFixture> gamesInRound, int dflRound) {
		
		List<DflRoundEarlyGames> earlyGames = new ArrayList<>();

		loggerUtils.log("info", "Calculating early games");
		
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
