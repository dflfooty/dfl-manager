package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflFixture;
import net.dflmngr.model.entity.DflLadder;
import net.dflmngr.model.entity.DflPlayerPredictedScores;
import net.dflmngr.model.entity.DflPlayerScores;
import net.dflmngr.model.entity.DflSelectedPlayer;
import net.dflmngr.model.entity.DflTeamScores;
import net.dflmngr.model.service.DflFixtureService;
import net.dflmngr.model.service.DflLadderService;
import net.dflmngr.model.service.DflPlayerPredictedScoresService;
import net.dflmngr.model.service.DflPlayerScoresService;
import net.dflmngr.model.service.DflSelectedTeamService;
import net.dflmngr.model.service.DflTeamScoresService;
import net.dflmngr.model.service.impl.DflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflLadderServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerPredictedScoresServiceImpl;
import net.dflmngr.model.service.impl.DflSelectedTeamServiceImpl;
import net.dflmngr.model.service.impl.DflTeamScoresServiceImpl;

public class LadderCalculatorHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "RoundProgress";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	DflLadderService dflLadderService;
	DflFixtureService dflFixtureService;
	DflTeamScoresService dflTeamScoresService;
	DflSelectedTeamService dflSelectedTeamService;
	DflPlayerScoresService dflPlayerScoresService;
	DflPlayerPredictedScoresService dflPlayerPredictedScoresService;
	
	public LadderCalculatorHandler() {
		dflLadderService = new DflLadderServiceImpl();
		dflFixtureService = new DflFixtureServiceImpl();
		dflTeamScoresService = new DflTeamScoresServiceImpl();
		dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		dflPlayerPredictedScoresService = new DflPlayerPredictedScoresServiceImpl();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round, boolean liveLadderOveride) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
						
			loggerUtils.log("info", "LadderCalculatorHandler executing round={} ...", round);			
			handleLadder(round, liveLadderOveride);
			
			dflLadderService.close();;
			dflFixtureService.close();;
			dflTeamScoresService.close();
			dflSelectedTeamService.close();
			dflPlayerScoresService.close();
			dflPlayerPredictedScoresService.close();
			
			loggerUtils.log("info", "LadderCalculatorHandler completed");
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
		
	private void handleLadder(int round, boolean liveLadderOveride) {
		
		List<DflFixture> roundFixtures = dflFixtureService.getFixturesForRound(round);
		Map<String, DflTeamScores> roundTeamScores = dflTeamScoresService.getForRoundWithKey(round);
		Map<String, DflLadder> previousLadder = dflLadderService.getForPeviousRoundWithKey(round);
		
		List<DflLadder> roundLadder = new ArrayList<>();
		
		int homeTeamScore;
		int awayTeamScore;
		
		for(DflFixture fixture : roundFixtures) {
			
			String homeTeamCode = fixture.getHomeTeam();
			String awayTeamCode = fixture.getAwayTeam();
			
			if(liveLadderOveride) {
				homeTeamScore = calculateLiveTeamScore(round, homeTeamCode);
				awayTeamScore = calculateLiveTeamScore(round, awayTeamCode);
			} else {
				homeTeamScore = roundTeamScores.get(homeTeamCode).getScore();
				awayTeamScore = roundTeamScores.get(awayTeamCode).getScore();				
			}
			
			DflLadder homeTeamLadder = calculateLadder(round, homeTeamCode, previousLadder.get(homeTeamCode), homeTeamScore, awayTeamScore, liveLadderOveride);
			DflLadder awayTeamLadder = calculateLadder(round, awayTeamCode, previousLadder.get(awayTeamCode), awayTeamScore, homeTeamScore, liveLadderOveride);
			
			roundLadder.add(homeTeamLadder);
			roundLadder.add(awayTeamLadder);
		}
		
		dflLadderService.replaceAllForRound(round, roundLadder);
	}
	
	private int calculateLiveTeamScore(int round, String teamCode) {
		
		int teamScore = 0;
		
		List<DflSelectedPlayer> selectedTeam = dflSelectedTeamService.getSelectedTeamForRound(round, teamCode);
		Map<Integer, DflPlayerScores> scores = dflPlayerScoresService.getForRoundWithKey(round);
		Map<Integer, DflPlayerPredictedScores> predictedScores = dflPlayerPredictedScoresService.getForRoundWithKey(round);
		
		for(DflSelectedPlayer player : selectedTeam) {
			if(!player.isDnp()) {
				if(player.hasPlayed()) {
					teamScore = teamScore + scores.get(player.getPlayerId()).getScore();
				} else {
					teamScore = teamScore + predictedScores.get(player.getPlayerId()).getPredictedScore();
				}
			}
		}
	
		return teamScore;
	}
	
	private DflLadder calculateLadder(int round, String teamCode, DflLadder previousLadder, int teamScore, int oppositionScore, boolean isLive) {
		
		DflLadder newLadder = new DflLadder();
		
		int wins = (teamScore > oppositionScore ? 1 : 0);
		int losses = (teamScore < oppositionScore ? 1 : 0);
		int draws = (teamScore == oppositionScore ? 1 : 0);
		int pointsFor = teamScore;
		float averageFor = (float)pointsFor / round;
		int pointsAgainst = oppositionScore;
		float averageAgainst = (float)pointsAgainst / round;
		int pts = (teamScore > oppositionScore ? 4 : (teamScore == oppositionScore ? 2 : 0));
		float percentage = ((float)pointsFor / pointsAgainst) * 100;
		
		if(round > 1) {
			wins = wins + previousLadder.getWins();
			losses = losses + previousLadder.getLosses();
			draws = draws + previousLadder.getDraws();
			pointsFor = pointsFor + previousLadder.getPointsFor();
			averageFor = (float)pointsFor / round;
			pointsAgainst = pointsAgainst + previousLadder.getPointsAgainst();
			averageAgainst = (float)pointsAgainst / round;
			pts = pts + previousLadder.getPts();
			percentage = ((float)pointsFor / pointsAgainst) * 100;
		}

		newLadder.setRound(round);
		newLadder.setTeamCode(teamCode);
		newLadder.setWins(wins);
		newLadder.setLosses(losses);
		newLadder.setDraws(draws);
		newLadder.setPointsFor(pointsFor);
		newLadder.setAverageFor(averageFor);
		newLadder.setPointsAgainst(pointsAgainst);
		newLadder.setAverageAgainst(averageAgainst);
		newLadder.setPts(pts);
		newLadder.setPercentage(percentage);
		newLadder.setLive(isLive);
		
		return newLadder;
	}

}
