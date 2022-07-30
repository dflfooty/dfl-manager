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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Function;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.DomainDecodes;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.DflRoundEarlyGames;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.entity.keys.AflFixturePK;
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
			int aflRoundsMax = aflFixtrureService.getMaxAflRound();

			loggerUtils.log("info", "Executing DflRoundInfoCalculator, max AFL rounds: {}", aflRoundsMax);
			loggerUtils.log("info", "Standard round lockout time: {}", standardLockout);
			
			List<DflRoundInfo> allRoundInfo = new ArrayList<>();
								
			int dflRound = 1;
			int aflRound = 1;
			
			while(aflRound <= aflRoundsMax) {
				
				loggerUtils.log("info", "Handling round: {}", aflRound);
				
				List<AflFixture> aflRoundFixtures = aflFixtrureService.getAflFixturesForRound(aflRound);
								
				if(aflRoundFixtures.size() == 9) {					
					allRoundInfo.add(handleFullRound(dflRound, aflRound, aflRoundFixtures));
				} else {
					List<DflRoundInfo> splitRoundInfo = handelSplitRound(dflRound, aflRound, aflRoundsMax, aflRoundFixtures);

					dflRound = resetDlfRound(dflRound, splitRoundInfo);
					aflRound = resetAflRound(aflRound, splitRoundInfo);
				}
				dflRound++;
				aflRound++;
			}
			
			dflRoundInfoService.replaceAll(allRoundInfo);
			
			loggerUtils.log("info", "DflRoundInfoCalculator Complete");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}

	private DflRoundInfo handleFullRound(int dflRound, int aflRound, List<AflFixture> aflRoundFixtures) {

		loggerUtils.log("info", "AFL full round");
							
		Map<Integer, ZonedDateTime> gameStartTimes = new HashMap<>();
		
		for(AflFixture fixture : aflRoundFixtures) {
			gameStartTimes.put(fixture.getGame(), fixture.getStartTime());
		}
				
		loggerUtils.log("info", "Creating round mapping: DFL rouund={}; AFL round={};", dflRound, aflRound);
		List<DflRoundMapping> roundMappings = new ArrayList<>();
		DflRoundMapping roundMapping = new DflRoundMapping();
		roundMapping.setRound(dflRound);
		roundMapping.setAflRound(aflRound);
		roundMappings.add(roundMapping);

		return createRoundInfo(dflRound, false, gameStartTimes, roundMappings, aflRoundFixtures);
	}

	private DflRoundInfo createRoundInfo(int dflRound, boolean splitRound, Map<Integer, ZonedDateTime> gameStartTimes, 
										 List<DflRoundMapping> roundMappings, List<AflFixture> aflFixtures) {
		
		DflRoundInfo dflRoundInfo = new DflRoundInfo();

		loggerUtils.log("info", "Creating DFL round info");
		
		dflRoundInfo.setRound(dflRound);
		
		if(splitRound) {
			dflRoundInfo.setSplitRound(DomainDecodes.DFL_ROUND_INFO.SPLIT_ROUND.YES);
		} else {
			dflRoundInfo.setSplitRound(DomainDecodes.DFL_ROUND_INFO.SPLIT_ROUND.NO);
		}
				
		loggerUtils.log("info", "AFL game start times: {}", gameStartTimes);
		
		loggerUtils.log("info", "Calculating hard lockout time");
		ZonedDateTime hardLockout = calculateHardLockout(dflRound, gameStartTimes);
		dflRoundInfo.setHardLockoutTime(hardLockout);
		
		loggerUtils.log("info", "Round mapping: DFL round={}; RoundMappings={};", dflRound, roundMappings);
		dflRoundInfo.setRoundMapping(roundMappings);
		
		loggerUtils.log("info", "Calculating early games");
		List<DflRoundEarlyGames> earlyGames = calculateEarlyGames(hardLockout, aflFixtures, dflRound);
		dflRoundInfo.setEarlyGames(earlyGames);

		return dflRoundInfo;

	}

	private List<DflRoundInfo> handelSplitRound(int dflRound, int aflRound, int aflRoundsMax, List<AflFixture> aflRoundFixtures) {
		List<DflRoundInfo> allRoundInfo = new ArrayList<>();

		loggerUtils.log("info", "AFL split/bye round");
					
		int gamesCount = aflRoundFixtures.size();
		loggerUtils.log("info", "Games in AFL round: {}", gamesCount);
		
		Map<Integer, List<AflFixture>> gamesInSplitRound = new HashMap<>();
		gamesInSplitRound.put(aflRound, aflRoundFixtures);
		
		boolean splitRoundHandled = false;
		while(!splitRoundHandled || aflRound <= aflRoundsMax) {
			aflRound++;

			List<AflFixture> aflNextRoundFixtures = aflFixtrureService.getAflFixturesForRound(aflRound);
			gamesInSplitRound.put(aflRound, aflNextRoundFixtures);
			
			loggerUtils.log("info", "Games in next AFL round: {}", aflNextRoundFixtures.size());
			gamesCount = gamesCount + aflNextRoundFixtures.size();
			
			if(gamesCount % 9 == 0)  {
				loggerUtils.log("info", "Found enough games to make DFL rounds ... calculation split round");
				allRoundInfo.addAll(calculateSplitRound(dflRound, gamesInSplitRound));
				splitRoundHandled = true;
			} 
		}

		return allRoundInfo;
	}

	private int resetDlfRound(int dflRound, List<DflRoundInfo> splitRoundInfo) {
		for(DflRoundInfo roundInfo : splitRoundInfo) {
			if(dflRound < roundInfo.getRound()) {
				dflRound = roundInfo.getRound();
			}
		}

		return dflRound;
	}

	private int resetAflRound(int aflRound, List<DflRoundInfo> splitRoundInfo) {
		for(DflRoundInfo roundInfo : splitRoundInfo) {
			for(DflRoundMapping roundMapping : roundInfo.getRoundMapping()) {
				if(aflRound < roundMapping.getAflRound()) {
					aflRound = roundMapping.getAflRound();
				}
			}
		}

		return aflRound;
	}
	
	private List<DflRoundInfo> calculateSplitRound(int dflRound, Map<Integer, List<AflFixture>> gamesInSplitRound) {
		
		List<DflRoundInfo> splitRoundInfo = new ArrayList<>();
		List<DflRoundMapping> roundMappings = new ArrayList<>();
		Map<String, AflFixture> workingWithGames = new HashMap<>();
		
		Set<Integer> aflSplitRounds = gamesInSplitRound.keySet();
		int aflFirstSplitRound = Collections.min(aflSplitRounds);
		
		loggerUtils.log("info", "AFL rounds used in split round: {}", aflSplitRounds);
				
		for(int i = 0; i < aflSplitRounds.size(); i++) {
			
			int currentAflSplitRound = aflFirstSplitRound + i;
			
			loggerUtils.log("info", "Working with AFL round: {}", currentAflSplitRound);
			
			getGamesToWorkWith(currentAflSplitRound, gamesInSplitRound, workingWithGames);
			
			if(roundMappings.size() == 18) {
				splitRoundInfo.add(createSplitRoundInfo(dflRound, roundMappings));
				roundMappings.clear();
				dflRound++;
			} else {
				mapGames(dflRound, workingWithGames, roundMappings);				

				for(DflRoundMapping roundMap : roundMappings) {
					String key = roundMap.getAflRound() + "-" + roundMap.getAflGame() + "-" + roundMap.getAflTeam();
					workingWithGames.remove(key);
				}
			}
		}
		
		return splitRoundInfo;
	}

	private void getGamesToWorkWith(int currentAflSplitRound, Map<Integer, List<AflFixture>> gamesInSplitRound, Map<String, AflFixture> workingWithGames) {
		List<AflFixture> splitRoundGames = gamesInSplitRound.get(currentAflSplitRound);
		for(AflFixture splitRoundGame : splitRoundGames) {
			Function<String, AflFixture> addFixture = key -> {
				loggerUtils.log("info", "Adding working with AFL game: {}", splitRoundGame);
				return splitRoundGame;
			};

			String homeKey = splitRoundGame.getRound() + "-" + splitRoundGame.getGame() + "-" + splitRoundGame.getHomeTeam();
			String awayKey = splitRoundGame.getRound() + "-" + splitRoundGame.getGame() + "-" + splitRoundGame.getAwayTeam();
			
			workingWithGames.computeIfAbsent(homeKey, addFixture);
			workingWithGames.computeIfAbsent(awayKey, addFixture);
		}
	}

	private List<DflRoundMapping> mapGames(int dflRound, Map<String, AflFixture> workingWithGames, List<DflRoundMapping> roundMappings) {		
		loggerUtils.log("info", "Find teams to fill out round");
		
		SortedSet<String> keys = new TreeSet<>(workingWithGames.keySet());
		for(String key : keys) {
			AflFixture aflFixture = workingWithGames.get(key);
			
			String aflTeam = key.split("-")[2]; 

			boolean teamAlreadyMapped = isTeamMapped(dflRound, aflTeam, roundMappings);
								
			if(!teamAlreadyMapped) {
				DflRoundMapping roundMapping = createRoundMapping(dflRound, aflTeam, aflFixture);
				roundMappings.add(roundMapping);
			}
		}
		
		return roundMappings;
	}

	private boolean isTeamMapped(int dflRound, String aflTeam, List<DflRoundMapping> roundMappings) {

		Iterator<DflRoundMapping> i = roundMappings.listIterator();
		boolean teamMapped = false;

		while(i.hasNext() && !teamMapped) {
			DflRoundMapping roundMap = i.next();
			if(aflTeam.equals(roundMap.getAflTeam())) {
				loggerUtils.log("info", "Team: {} already mapped in round: {}", aflTeam, dflRound);
				teamMapped = true;
			}
		}

		return teamMapped;
	}

	private DflRoundMapping createRoundMapping(int dflRound, String aflTeam, AflFixture aflFixture) {
		DflRoundMapping roundMapping = new DflRoundMapping();
						
		roundMapping.setRound(dflRound);
		roundMapping.setAflRound(aflFixture.getRound());
		roundMapping.setAflGame(aflFixture.getGame());					
		roundMapping.setAflTeam(aflTeam);

		loggerUtils.log("info", "Round mapping created: {}", roundMapping);

		return roundMapping;
	}

	private DflRoundInfo createSplitRoundInfo(int dflRound, List<DflRoundMapping> roundMappings) {
		
		List<AflFixture> mappedAflFixtures = new ArrayList<>();
		Map<Integer, ZonedDateTime> gameStartTimes = new HashMap<>();

		for(DflRoundMapping roundMapping : roundMappings) {
			AflFixturePK aflFixturePK = new AflFixturePK();
			aflFixturePK.setRound(roundMapping.getAflRound());
			aflFixturePK.setGame(roundMapping.getAflGame());
			
			AflFixture aflFixture = aflFixtrureService.get(aflFixturePK);

			if(!mappedAflFixtures.contains(aflFixture)) {
				mappedAflFixtures.add(aflFixture);	
			}

			Integer gameStartTimeKey = Integer.parseInt(Integer.toString(aflFixture.getRound()) + Integer.toString(aflFixture.getGame()));
			gameStartTimes.computeIfAbsent(gameStartTimeKey, k -> aflFixture.getStartTime());
		}
		
		return createRoundInfo(dflRound, true, gameStartTimes, roundMappings, mappedAflFixtures);
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
				throw new IllegalStateException("No non-standard lockout defined");
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
