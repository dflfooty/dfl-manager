package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.DflFixture;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflRoundEarlyGames;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflFixtureService;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.DflTeamPredictedScoresService;
import net.dflmngr.model.service.DflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.InsAndOutsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPredictedScoresServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.InsAndOutsServiceImpl;
import net.dflmngr.utils.EmailUtils;

public class StartRoundHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "StartRound";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	DflTeamService dflTeamService;
	GlobalsService globalsService;
	DflFixtureService dflFixtureService;
	DflTeamPredictedScoresService dflTeamPredictedScoresService;
	DflPlayerService dflPlayerService;
	InsAndOutsService insAndOutsService;
	DflTeamPlayerService dflTeamPlayerService;
	DflRoundInfoService dflRoundInfoService;
	AflFixtureService aflFixtureService;
	
	String emailOverride;
	
	public StartRoundHandler() {
		dflTeamService = new DflTeamServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflFixtureService = new DflFixtureServiceImpl();
		dflTeamPredictedScoresService = new DflTeamPredictedScoresServiceImpl();
		dflPlayerService = new DflPlayerServiceImpl();
		insAndOutsService = new InsAndOutsServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		dflRoundInfoService = new DflRoundInfoServiceImpl();
		aflFixtureService = new AflFixtureServiceImpl();
		
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round, String emailOveride, boolean fromScoresCalculator) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			if(emailOveride != null && !emailOveride.equals("")) {
				this.emailOverride = emailOveride;
			}
			
			loggerUtils.log("info", "Starting round={}", round);
			
			DflRoundInfo roundInfo = dflRoundInfoService.get(round);
			
			boolean earlyGamesCompleted = false;
			int earlyGameCompletedCount = 0;
			
			for(DflRoundEarlyGames earlyGame : roundInfo.getEarlyGames()) {
				AflFixture fixture = aflFixtureService.getPlayedGame(earlyGame.getAflRound(), earlyGame.getAflGame());
				
				if(fixture != null) {
					earlyGameCompletedCount++;
				}
			}
			
			if((roundInfo.getEarlyGames() == null) || (earlyGameCompletedCount == roundInfo.getEarlyGames().size())) {
				earlyGamesCompleted = true;
			}

			
			if(!fromScoresCalculator && earlyGamesCompleted) {
				loggerUtils.log("info", "No early games or early games completed, sending start round email.");
				sendFirstGameEmail(round, emailOveride);
			}
			
			loggerUtils.log("info", "Start round completed");
			
			dflTeamService.close();
			globalsService.close();
			dflFixtureService.close();
			dflTeamPredictedScoresService.close();
			dflPlayerService.close();
			insAndOutsService.close();
			dflTeamPlayerService.close();
			dflRoundInfoService.close();
			aflFixtureService.close();
		
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
		
	private void sendFirstGameEmail(int round, String emailOveride) throws Exception {
		
		String dflMngrEmail = globalsService.getEmailConfig().get("dflmngrEmailAddr");
		
		String subject = "DFL Manager - Predictions";
		
		List<DflFixture> roundFixtures = dflFixtureService.getFixturesForRound(round);
		
		String body = "<html>\n<body>\n";
		body = "<p>This week in round " + round + "</p>\n";
		body = body + "<p>DFL Manager has made the following predictions:</p>\n";
		body = body + "<p><ul type=none>\n";
						
		for(DflFixture fixture : roundFixtures) {
			DflTeam homeTeam = dflTeamService.get(fixture.getHomeTeam());
			int homeTeamPredictedScore = dflTeamPredictedScoresService.getTeamPredictedScoreForRound(homeTeam.getTeamCode(), round).getPredictedScore();
			
			DflTeam awayTeam = dflTeamService.get(fixture.getAwayTeam());
			int awayTeamPredictedScore = dflTeamPredictedScoresService.getTeamPredictedScoreForRound(awayTeam.getTeamCode(), round).getPredictedScore();
			
			String resultString = "";
			if(homeTeamPredictedScore > awayTeamPredictedScore) {
				resultString = " to defeat ";
			} else {
				resultString = " to be defeated by ";
			}
			
			String gameUrl = globalsService.getOnlineBaseUrl() + "/results/" + fixture.getRound() + "/" + fixture.getGame();
			
			body = body + "<li>" + homeTeam.getName() + " " + resultString + awayTeam.getName() + ", " + homeTeamPredictedScore + " to " + awayTeamPredictedScore + 
				   " - <a herf='" + gameUrl + "'>Match Report</a></li>\n";
		}
		
		body = body + "</ul></p>\n";
				
		List<DflTeam> teams = dflTeamService.findAll();
		
		body = body + selectionSummary(round, teams);
		
		body = body + "<p>DFL Manager Admin</p>\n";
		body = body + "</body>\n</html>";
		
		List<String> to = new ArrayList<>();
		
		if(emailOverride != null && !emailOverride.equals("")) {
			to.add(emailOverride);
		} else {
			for(DflTeam team : teams) {
				to.add(team.getCoachEmail());
			}
		}

		loggerUtils.log("info", "Emailing early games start round to={}", to);
		EmailUtils.sendHtmlEmail(to, dflMngrEmail, subject, body, null);	
	}
	
	private String selectionSummary(int round, List<DflTeam> teams) {
		
		String text = "<br><p><b>Selection Summary</b></p>\n";
		
		for(DflTeam team : teams) {
			List<InsAndOuts> insAndOuts = insAndOutsService.getByTeamAndRound(round, team.getTeamCode());
			List<InsAndOuts> ins = new ArrayList<>();
			List<InsAndOuts> outs = new ArrayList<>();
			
			InsAndOuts emg1 = null;
			InsAndOuts emg2 = null;
			
			for(InsAndOuts selection : insAndOuts) {
				switch(selection.getInOrOut()) {
					case "I" : ins.add(selection); break;
					case "O" : outs.add(selection); break;
					case "E1" : emg1 = selection; break;
					case "E2" : emg2 = selection; break;
				}
			}
			
			text = text + "<p><b>" + team.getShortName() + ":</b>\n";
			
			if(ins.isEmpty() && outs.isEmpty() && emg1 == null && emg2 == null) {
				text = text + "No selections received<br>\n";
			} else {
				text = text + "<br>\n";

				if(!ins.isEmpty()) {
					String line = "";
					
					text = text + "<b>Ins: </b>\n";
					
					for(InsAndOuts in : ins) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), in.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());
						
						if(line.length() == 0) {
							line = in.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + in.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					
					text = text + line + "<br>\n";
				}
				if(!outs.isEmpty()) {
					String line = "";
					
					text = text + "<b>Outs</b>\n";
					
					for(InsAndOuts out : outs) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), out.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());

						if(line.length() == 0) {
							line = out.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + out.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					
					text = text + line + "<br>\n";
				}
				if(emg1 != null && emg2 != null) {
					String line = "";
					
					text = text + "<b>Emgs</b>\n";
					if(emg1 != null) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), emg1.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());

						if(line.length() == 0) {
							line = emg1.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + emg2.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					if(emg2 != null) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), emg2.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());

						if(line.length() == 0) {
							line = emg2.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + emg2.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					
					text = text + line + "<br>\n";
				}
			}
			
			text = text + "</p>\n";
		}
		
		text = text + "<br>\n";
		
		return text;
	}
	
	public static void main(String[] args) {
		
		try {
			String email = null;
			int round = 0;
			
			if(args.length > 2 || args.length < 1) {
				System.out.println("usage: RawStatsReport <round> optional [<email>]");
			} else {
				
				round = Integer.parseInt(args[0]);
				
				if(args.length == 2) {
					email = args[1];
				}
				
				StartRoundHandler startRound = new StartRoundHandler();
				startRound.configureLogging("batch.name", "batch-logger", "StartRound");
				startRound.execute(round, email, false);
			}
			
			System.exit(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
