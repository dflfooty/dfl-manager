package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflPlayerPredictedScores;
import net.dflmngr.model.entity.DflPlayerScores;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.entity.DflSelectedPlayer;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.entity.DflTeamScores;
import net.dflmngr.model.entity.RawPlayerStats;
import net.dflmngr.model.entity.keys.DflPlayerScoresPK;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflPlayerPredictedScoresService;
import net.dflmngr.model.service.DflPlayerScoresService;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.DflSelectedTeamService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.DflTeamScoresService;
import net.dflmngr.model.service.DflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.RawPlayerStatsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerPredictedScoresServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerScoresServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.DflSelectedTeamServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamScoresServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.RawPlayerStatsServiceImpl;
import net.dflmngr.utils.DflmngrUtils;

public class ScoresCalculatorHandler {
	private LoggingUtils loggerUtils;

	boolean isExecutable;

	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "RoundProgress";

	String mdcKey;
	String loggerName;
	String logfile;

	RawPlayerStatsService rawPlayerStatsService;
	DflPlayerService dflPlayerService;
	DflTeamPlayerService dflTeamPlayerService;
	DflPlayerScoresService dflPlayerScoresService;
	DflSelectedTeamService dflSelectedTeamService;
	DflTeamService dflTeamService;
	DflTeamScoresService dflTeamScoresService;
	DflRoundInfoService dflRoundInfoService;
	AflFixtureService aflFixtureService;
	GlobalsService globalsService;
	DflPlayerPredictedScoresService dflPlayerPredictedScoresService;

	public ScoresCalculatorHandler() {
		rawPlayerStatsService = new RawPlayerStatsServiceImpl();
		dflPlayerService = new DflPlayerServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		dflPlayerScoresService = new DflPlayerScoresServiceImpl();
		dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		dflTeamService = new DflTeamServiceImpl();
		dflTeamScoresService = new DflTeamScoresServiceImpl();
		dflRoundInfoService = new DflRoundInfoServiceImpl();
		aflFixtureService = new AflFixtureServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflPlayerPredictedScoresService = new DflPlayerPredictedScoresServiceImpl();
	}

	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}

	public void execute(int round) {

		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			loggerUtils.log("info", "ScoresCalculator executing round={} ...", round);
			loggerUtils.log("info", "Handling player scores");
			handlePlayerScores(round);

			DflRoundInfo dflRoundInfo = dflRoundInfoService.get(round);

			Map<Integer, DflPlayerPredictedScores> predictedScores = dflPlayerPredictedScoresService.getForRoundWithKey(round);

			loggerUtils.log("info", "Handling team scores");
			handleTeamScores(round, dflRoundInfo, predictedScores);

			rawPlayerStatsService.close();
			dflPlayerService.close();
			dflTeamPlayerService.close();
			dflPlayerScoresService.close();
			dflSelectedTeamService.close();
			dflTeamService.close();
			dflTeamScoresService.close();
			dflRoundInfoService.close();
			aflFixtureService.close();
			globalsService.close();
			dflPlayerPredictedScoresService.close();

			loggerUtils.log("info", "ScoresCalculator completed");

		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}

	private void handlePlayerScores(int round) {

		Map<String, RawPlayerStats> stats = rawPlayerStatsService.getForRoundWithKey(round);
		List<DflPlayerScores> scores = new ArrayList<>();

		for (Map.Entry<String, RawPlayerStats> entry : stats.entrySet()) {

			DflPlayerScores playerScores = new DflPlayerScores();
			String aflPlayerId = entry.getKey();
			RawPlayerStats playerStats = entry.getValue();

			int score = calculatePlayerScore(playerStats);

			DflPlayer dflPlayer = dflPlayerService.getByAflPlayerId(aflPlayerId);

			if(dflPlayer == null) {
				loggerUtils.log("info", "Missing afl dfl player mapping: aflPlayerId={};", aflPlayerId);
			} else {
				DflTeamPlayer dflTeamPlayer = dflTeamPlayerService.get(dflPlayer.getPlayerId());

				playerScores.setPlayerId(dflPlayer.getPlayerId());
				playerScores.setRound(round);
				playerScores.setAflPlayerId(aflPlayerId);

				if(dflTeamPlayer != null) {
					playerScores.setTeamCode(dflTeamPlayer.getTeamCode());
					playerScores.setTeamPlayerId(dflTeamPlayer.getTeamPlayerId());
				}

				playerScores.setScore(score);

				loggerUtils.log("info", "Player score={}", playerScores);
				scores.add(playerScores);
			}
		}

		dflPlayerScoresService.replaceAllForRound(round, scores);
	}

	private int calculatePlayerScore(RawPlayerStats playerStats) {

		int score = 0;

		int disposals = playerStats.getDisposals();
		int marks = playerStats.getMarks();
		int hitOuts = playerStats.getHitouts();
		int freesFor = playerStats.getFreesFor();
		int fressAgainst = playerStats.getFreesAgainst();
		int tackles = playerStats.getTackles();
		int goals = playerStats.getGoals();

		score = disposals + marks + hitOuts + freesFor + (-fressAgainst) + tackles + (goals * 3);

		return score;
	}

	private void handleTeamScores(int round, DflRoundInfo dflRoundInfo, Map<Integer, DflPlayerPredictedScores> predictedScores) throws Exception {

		List<DflTeam> teams = dflTeamService.findAll();
		List<DflTeamScores> scores = new ArrayList<>();

		for(DflTeam team : teams) {

			List<DflSelectedPlayer> selectedTeam = dflSelectedTeamService.getSelectedTeamForRound(round, team.getTeamCode());
			DflTeamScores teamScore = new DflTeamScores();

			int score = calculateTeamScore(selectedTeam, dflRoundInfo, predictedScores);

			teamScore.setTeamCode(team.getTeamCode());
			teamScore.setRound(round);
			teamScore.setScore(score);

			loggerUtils.log("info", "Team score={}", teamScore);
			scores.add(teamScore);
		}

		dflTeamScoresService.replaceAllForRound(round, scores);
	}

	private int calculateTeamScore(List<DflSelectedPlayer> selectedTeam, DflRoundInfo dflRoundInfo, Map<Integer, DflPlayerPredictedScores> predictedScores) throws Exception {

		int teamScore = 0;

		List<DflSelectedPlayer> played22 = new ArrayList<>();
		List<DflSelectedPlayer> emergencies = new ArrayList<>();
		List<DflSelectedPlayer> dnpPlayers = new ArrayList<>();
		List<DflSelectedPlayer> replacedDnpPlayers = new ArrayList<>();
		Map<Integer, Integer> scores = new HashMap<>();

		Map<Integer, String> playerPositions = new HashMap<>();
		List<String> benchPositions = new ArrayList<>();

		List<String> playedTeams = getPlayedTeams(dflRoundInfo);

		int ffCount = 0;
		int fwdCount = 0;
		int midCount = 0;
		int defCount = 0;
		int fbCount = 0;
		int rckCount = 0;

		for(DflSelectedPlayer selectedPlayer : selectedTeam) {
			DflPlayerScoresPK pk = new DflPlayerScoresPK();
			pk.setPlayerId(selectedPlayer.getPlayerId());
			pk.setRound(selectedPlayer.getRound());
			DflPlayerScores playerScore = dflPlayerScoresService.get(pk);

			DflPlayer player = dflPlayerService.get(selectedPlayer.getPlayerId());

			String position = player.getPosition().toLowerCase();
			playerPositions.put(selectedPlayer.getPlayerId(), position);

			if(selectedPlayer.isEmergency() == 0) {
				switch(position) {
					case "ff" :
						ffCount++;
						break;
					case "fwd" :
						fwdCount++;
						break;
					case "rck" :
						rckCount++;
						break;
					case "mid" :
						midCount++;
						break;
					case "def" :
						defCount++;
						break;
					case "fb" :
						fbCount++;
						break;
				}
			}

			selectedPlayer.setReplacementInd(null);

			if(playerScore == null) {
				loggerUtils.log("info", "Selected player: team={} teamPlayerId={} playerId={} has no score recorded",
								selectedPlayer.getTeamCode(), selectedPlayer.getTeamPlayerId(), selectedPlayer.getPlayerId());
				if(playedTeams.contains(DflmngrUtils.dflAflTeamMap.get(player.getAflClub()))) {
					loggerUtils.log("info", "AFL team as player marking as DNP");
					selectedPlayer.setDnp(true);
					selectedPlayer.setScoreUsed(false);
					selectedPlayer.setHasPlayed(true);
					dnpPlayers.add(selectedPlayer);
				} else {
					loggerUtils.log("info", "Checking if average will be used");
					int round = globalsService.getUseAverage(player.getAflClub());

					if(round == selectedPlayer.getRound()) {
						int score = predictedScores.get(selectedPlayer.getPlayerId()).getPredictedScore();
						scores.put(selectedPlayer.getPlayerId(), score);

						selectedPlayer.setHasPlayed(true);

						loggerUtils.log("info", "Using average score={}", score);

						if(selectedPlayer.isEmergency() == 0) {
							selectedPlayer.setScoreUsed(true);
							played22.add(selectedPlayer);
						} else {
							selectedPlayer.setScoreUsed(false);
							emergencies.add(selectedPlayer);
						}
					}
				}
			} else {
				loggerUtils.log("info", "Selected player: team={} teamPlayerId={} playerId={} has score recorded",
								selectedPlayer.getTeamCode(), selectedPlayer.getTeamPlayerId(), selectedPlayer.getPlayerId());

				loggerUtils.log("info", "Using score={}", playerScore.getScore());

				scores.put(selectedPlayer.getPlayerId(), playerScore.getScore());

				if(playedTeams.contains(DflmngrUtils.dflAflTeamMap.get(player.getAflClub()))) {
					selectedPlayer.setHasPlayed(true);
				} else {
					selectedPlayer.setHasPlayed(false);
				}
				selectedPlayer.setDnp(false);
				if(selectedPlayer.isEmergency() == 0) {
					selectedPlayer.setScoreUsed(true);
					played22.add(selectedPlayer);
				} else {
					selectedPlayer.setScoreUsed(false);
					emergencies.add(selectedPlayer);
				}

			}
		}

		loggerUtils.log("info", "Played 22={} -- Size:{}", played22, played22.size());
		loggerUtils.log("info", "DNPs={} -- Size:{}", dnpPlayers, dnpPlayers.size());
		loggerUtils.log("info", "Emergencies={} -- Size:{}", emergencies, emergencies.size());

		Comparator<DflSelectedPlayer> emgsComparator = Comparator.comparingInt(DflSelectedPlayer::isEmergency);
		emergencies.sort(emgsComparator);

		if(ffCount == 2) {
			benchPositions.add("ff");
		}
		if(fwdCount == 6) {
			benchPositions.add("fwd");
		}
		if(midCount == 6) {
			benchPositions.add("mid");
		}
		if(defCount == 6) {
			benchPositions.add("def");
		}
		if(fbCount == 2) {
			benchPositions.add("fb");
		}
		if(rckCount == 2) {
			benchPositions.add("rck");
		}

		loggerUtils.log("info", "Bench positions={}", benchPositions);

		if(!dnpPlayers.isEmpty()) {
			if(emergencies.isEmpty()) {
				loggerUtils.log("info", "No emergencies to replace DNPs");
				for(DflSelectedPlayer dnpPlayer : dnpPlayers) {
					if(dnpPlayer.isEmergency() == 0) {
						dnpPlayer.setScoreUsed(true);
						played22.add(dnpPlayer);
					}
					replacedDnpPlayers.add(dnpPlayer);
				}
				dnpPlayers.removeAll(replacedDnpPlayers);
			} else {
				loggerUtils.log("info", "Replacing DNPs by position");
				for(DflSelectedPlayer dnpPlayer : dnpPlayers) {
					DflSelectedPlayer replacement = null;

					if(dnpPlayer.isEmergency() == 0) {
						for(DflSelectedPlayer emergency : emergencies) {
							String dnpPosition = playerPositions.get(dnpPlayer.getPlayerId());
							String emgPosition = playerPositions.get(emergency.getPlayerId());

							if(dnpPosition.equals(emgPosition)) {
								replacement = emergency;
								break;
							}
						}

						if(replacement != null) {
							emergencies.remove(replacement);
							replacement.setScoreUsed(true);
							if(replacement.isEmergency() == 1) {
								replacement.setReplacementInd("*");
								dnpPlayer.setReplacementInd("*");
							} else {
								replacement.setReplacementInd("**");
								dnpPlayer.setReplacementInd("**");
							}
							played22.add(replacement);
							replacedDnpPlayers.add(dnpPlayer);
							loggerUtils.log("info", "Replacing DNP={} with Emergency={} based on position", dnpPlayer, replacement);
						}
					} else {
						loggerUtils.log("info", "DNP is an emergency, no need to replace.  DNP={}", dnpPlayer);
						replacedDnpPlayers.add(dnpPlayer);
					}
				}
				dnpPlayers.removeAll(replacedDnpPlayers);
				if(!dnpPlayers.isEmpty()) {
					loggerUtils.log("info", "Replacing DNPs by promoting bench players");

					for(DflSelectedPlayer dnpPlayer : dnpPlayers) {
						DflSelectedPlayer replacement = null;

						String dnpPosition = playerPositions.get(dnpPlayer.getPlayerId());

						for(DflSelectedPlayer emergency : emergencies) {
							if(benchPositions.size() == 4) {
								if(benchPositions.contains(dnpPosition)) {
									replacement = emergency;
									break;
								}
							} else {
								String emgPosition = playerPositions.get(emergency.getPlayerId());

								switch(emgPosition) {
									case "ff":
										if(ffCount < 2) {
											replacement = emergency;
										}
										break;
									case "fwd":
										if(fwdCount < 6) {
											replacement = emergency;
										}
										break;
									case "rck":
										if(rckCount < 2) {
											replacement = emergency;
										}
										break;
									case "mid":
										if(midCount < 6) {
											replacement = emergency;
										}
										break;
									case "fb":
										if(fbCount < 2) {
											replacement = emergency;
										}
										break;
									case "def":
										if(defCount < 6) {
											replacement = emergency;
										}
										break;
								}
							}
						}

						if(replacement != null) {
							emergencies.remove(replacement);
							replacement.setScoreUsed(true);
							if(replacement.isEmergency() == 1) {
								replacement.setReplacementInd("*");
								dnpPlayer.setReplacementInd("*");
							} else {
								replacement.setReplacementInd("**");
								dnpPlayer.setReplacementInd("**");
							}
							played22.add(replacement);
							replacedDnpPlayers.add(dnpPlayer);
							loggerUtils.log("info", "Bench can take the ground DNP={} with Emergency={}", dnpPlayer, replacement);
						} else {
							dnpPlayer.setScoreUsed(true);
							played22.add(dnpPlayer);
							loggerUtils.log("info", "Positional rules don't allow emergency DNP={} with Emergency={}", dnpPlayer, replacement);
						}
					}
				}
				dnpPlayers.removeAll(replacedDnpPlayers);
			}
		}

		for(DflSelectedPlayer player : played22) {
			if(!player.isDnp()) {
				if(scores.containsKey(player.getPlayerId())) {
					teamScore = teamScore + scores.get(player.getPlayerId());
					loggerUtils.log("info", "Calculating scores team={}, playerId={}. teamplayer={}, playerscore={}, teamscore={}",
					         player.getTeamCode(), player.getPlayerId(), player.getTeamPlayerId(), scores.get(player.getPlayerId()), teamScore);
				}
			}
		}

		dflSelectedTeamService.updateAll(played22, false);
		if(!emergencies.isEmpty()) {
			dflSelectedTeamService.updateAll(emergencies, false);
		}
		if(!replacedDnpPlayers.isEmpty()) {
			dflSelectedTeamService.updateAll(replacedDnpPlayers, false);
		}

		return teamScore;
	}

	private List<String> getPlayedTeams(DflRoundInfo dflRoundInfo) throws Exception {
		List<String> playedTeams = new ArrayList<>();
		int aflRound = 0;
		for(DflRoundMapping roundMapping : dflRoundInfo.getRoundMapping()) {
			int currentAflRound = roundMapping.getAflRound();

			if(roundMapping.getAflGame() == 0) {
				if(aflRound != currentAflRound) {
					playedTeams.addAll(aflFixtureService.getAflTeamsPlayedForRound(currentAflRound));
					aflRound = currentAflRound;
				}
			} else {
				AflFixture aflFixture = aflFixtureService.getPlayedGame(currentAflRound, roundMapping.getAflGame());
				if(aflFixture != null) {
					playedTeams.add(roundMapping.getAflTeam());
					aflRound = currentAflRound;
				}
			}
		}

		return playedTeams;
	}

	public static void main(String[] args) {

		Options options = new Options();
		Option roundOpt  = Option.builder("r").argName("round").hasArg().desc("round to run on").type(Number.class).required().build();
		options.addOption(roundOpt);

		try {
			int round = 0;

			CommandLineParser parser = new DefaultParser();
			CommandLine cli = parser.parse(options, args);

			round = ((Number)cli.getParsedOptionValue("r")).intValue();

			ScoresCalculatorHandler scoresCalculatorHandler = new ScoresCalculatorHandler();
			scoresCalculatorHandler.configureLogging("batch.name", "batch-logger", ("ScoresCalculatorHandler_R" + round));
			scoresCalculatorHandler.execute(round);
		} catch (ParseException ex) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "ScoresCalculatorHandler", options);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
