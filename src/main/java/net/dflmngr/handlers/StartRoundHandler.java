package net.dflmngr.handlers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import net.dflmngr.jndi.JndiProvider;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.DomainDecodes;
import net.dflmngr.model.entity.DflEarlyInsAndOuts;
import net.dflmngr.model.entity.DflFixture;
import net.dflmngr.model.entity.DflRoundEarlyGames;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflSelectedPlayer;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.service.DflEarlyInsAndOutsService;
import net.dflmngr.model.service.DflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.DflSelectedTeamService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.DflTeamPredictedScoresService;
import net.dflmngr.model.service.DflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.InsAndOutsService;
import net.dflmngr.model.service.impl.DflEarlyInsAndOutsServiceImpl;
import net.dflmngr.model.service.impl.DflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.DflSelectedTeamServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPredictedScoresServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.InsAndOutsServiceImpl;
import net.dflmngr.reports.InsAndOutsReport;
import net.dflmngr.utils.DflmngrUtils;
import net.dflmngr.utils.EmailUtils;
import net.dflmngr.validation.SelectedTeamValidation;

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
	DflSelectedTeamService dflSelectedTeamService;
	InsAndOutsService insAndOutsService;
	DflTeamPlayerService dflTeamPlayerService;
	DflEarlyInsAndOutsService dflEarlyInsAndOutsService;
	DflRoundInfoService dflRoundInfoService;
	GlobalsService globalsService;
	DflFixtureService dflFixtureService;
	DflTeamPredictedScoresService dflTeamPredictedScoresService;
	
	String emailOverride;
	
	public StartRoundHandler() {
		dflTeamService = new DflTeamServiceImpl();
		dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		insAndOutsService = new InsAndOutsServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		dflEarlyInsAndOutsService = new DflEarlyInsAndOutsServiceImpl();
		dflRoundInfoService = new DflRoundInfoServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflFixtureService = new DflFixtureServiceImpl();
		dflTeamPredictedScoresService = new DflTeamPredictedScoresServiceImpl();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
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

			boolean earlyGamesExist = false;
			boolean earlyGamesCompleted = false;
			
			DflRoundInfo dflRoundInfo = dflRoundInfoService.get(round);
			ZonedDateTime dummyReceivedDate = dflRoundInfo.getHardLockoutTime().minusMinutes(10);
			
			ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
			
			List<DflRoundEarlyGames> earlyGames = dflRoundInfo.getEarlyGames();
			
			if(earlyGames != null && dflRoundInfo.getEarlyGames().size() > 0) {
				loggerUtils.log("info", "Early games exist");
				earlyGamesExist = true;

				if(now.isAfter(dflRoundInfo.getHardLockoutTime())) {
					earlyGamesCompleted = true;
				}
			}
			
			if(earlyGamesExist && !earlyGamesCompleted) {
				loggerUtils.log("info", "Executing start round for round={} with earlyGameStart",  round);
			} else {
				loggerUtils.log("info", "Executing start round for round={}",  round);
			}
			
			createTeamSelections(round, earlyGamesExist, earlyGamesCompleted, dummyReceivedDate);
			
			loggerUtils.log("info", "Creating predictions");
			PredictionHandler predictions = new PredictionHandler();
			predictions.configureLogging(mdcKey, loggerName, logfile);
			predictions.execute(round);
			
			if(!earlyGamesExist || earlyGamesCompleted) {
				loggerUtils.log("info", "Creating insAndOuts Report");
				
				InsAndOutsReport insAndOutsReport = new InsAndOutsReport();
				insAndOutsReport.configureLogging(mdcKey, loggerName, logfile);
				insAndOutsReport.execute(round, "Full", emailOveride);
			} else {
				if(!fromScoresCalculator) {
					sendFirstGameEmail(round, emailOveride);
				}
			}
			
			if(earlyGamesExist && !earlyGamesCompleted) {
				loggerUtils.log("info", "Early game start round completed");
			} else {
				loggerUtils.log("info", "Start round completed");
			}
			
			dflTeamService.close();
			dflSelectedTeamService.close();
			insAndOutsService.close();
			dflTeamPlayerService.close();
			dflEarlyInsAndOutsService.close();
			dflRoundInfoService.close();
			globalsService.close();
		
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
		
	}
	
	private void createTeamSelections(int round, boolean earlyGamesExist, boolean earlyGamesCompleted, ZonedDateTime dummyReceivedDate) throws Exception {

		loggerUtils.log("info", "Creating team selections");
		
		List<DflTeam> teams = dflTeamService.findAll();
		
		for(DflTeam team : teams) {
			loggerUtils.log("info", "Working with team={}", team.getTeamCode());
			
			List<DflSelectedPlayer> tmpSelectedTeam = null;
			
			if(round == 1) {
				tmpSelectedTeam = new ArrayList<>();
				loggerUtils.log("info", "Round 1: no previous team");
			} else {
				tmpSelectedTeam = dflSelectedTeamService.getSelectedTeamForRound(round-1, team.getTeamCode());
				loggerUtils.log("info", "Not round 1: previous team: {}", tmpSelectedTeam);
			}
						
			List<InsAndOuts> insAndOuts = insAndOutsService.getByTeamAndRound(round, team.getTeamCode());
			loggerUtils.log("info", "Final ins and outs: {}", insAndOuts);
			
			//if((earlyGamesExist && earlyGamesCompleted) && (insAndOuts == null || insAndOuts.size() == 0)) {
			if(earlyGamesExist && (insAndOuts == null || insAndOuts.size() == 0)) {
				List<DflEarlyInsAndOuts> earlyInsAndOuts = dflEarlyInsAndOutsService.getByTeamAndRound(round, team.getTeamCode());
				
				loggerUtils.log("info", "Early ins and outs: {}", earlyInsAndOuts);
				
				if(earlyInsAndOuts != null && earlyInsAndOuts.size() > 0) {
					loggerUtils.log("info", "Early Ins and Outs, starting to validate");
					
					Map<String, List<Integer>> validationInsAndOuts = new HashMap<>();
					List<Integer> ins = new ArrayList<>();
					List<Integer> outs = new ArrayList<>();
					List<Double> emgs = new ArrayList<>();
					for(DflEarlyInsAndOuts inOrOut : earlyInsAndOuts) {
						if(inOrOut.getInOrOut().equals("I")) {
							ins.add(inOrOut.getTeamPlayerId());
						} else if (inOrOut.getInOrOut().equals("O")) {
							outs.add(inOrOut.getTeamPlayerId());
						} else if (inOrOut.getInOrOut().equals("E1")) {
							emgs.add(inOrOut.getTeamPlayerId() + 0.1);
						} else {
							emgs.add(inOrOut.getTeamPlayerId() + 0.2);
						}
					}
					validationInsAndOuts.put("in", ins);
					validationInsAndOuts.put("out", outs);
					
					SelectedTeamValidationHandler validationHandler = new SelectedTeamValidationHandler();
					validationHandler.configureLogging(mdcKey, loggerName, logfile);
					SelectedTeamValidation validationResult = validationHandler.execute(round, team.getTeamCode(), validationInsAndOuts, emgs, dummyReceivedDate, true);
					
					if(validationResult.isValid()) {
						loggerUtils.log("info", "Early Ins and Outs are valid and are being used");
							
						List<DflSelectedPlayer> oldEmergencies = new ArrayList<>();
						
						loggerUtils.log("info", "Removing previous emergencies");
						for(DflSelectedPlayer player : tmpSelectedTeam) {
							if(player.isEmergency() != 0) {
								loggerUtils.log("info", "Previous emergency seletecPlayer={}", player);
								oldEmergencies.add(player);
							}
						}
						
						tmpSelectedTeam.removeAll(oldEmergencies);
						
						for(DflEarlyInsAndOuts inOrOut : earlyInsAndOuts) {
							if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN)) {
								DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), inOrOut.getTeamPlayerId());
								
								DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
								selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
								selectedPlayer.setRound(round);
								selectedPlayer.setTeamCode(team.getTeamCode());
								selectedPlayer.setTeamPlayerId(inOrOut.getTeamPlayerId());
								selectedPlayer.setEmergency(0);
								selectedPlayer.setDnp(false);
								selectedPlayer.setScoreUsed(true);
								
								loggerUtils.log("info", "Adding player to selected team: player={}", selectedPlayer);
								tmpSelectedTeam.add(selectedPlayer);
							} else if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT)) {
								DflSelectedPlayer droppedPlayer = null;
								for(DflSelectedPlayer selectedPlayer : tmpSelectedTeam) {
									if(inOrOut.getTeamPlayerId() == selectedPlayer.getTeamPlayerId()) {
										droppedPlayer = selectedPlayer;
										loggerUtils.log("info", "Dropping player from selected team: player={}", droppedPlayer);
										break;
									}
								}
								tmpSelectedTeam.remove(droppedPlayer);
							} else {
								DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), inOrOut.getTeamPlayerId());
								
								DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
								selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
								selectedPlayer.setRound(round);
								selectedPlayer.setTeamCode(team.getTeamCode());
								selectedPlayer.setTeamPlayerId(inOrOut.getTeamPlayerId());
								
								if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1)) {
									selectedPlayer.setEmergency(1);
								} else {
									selectedPlayer.setEmergency(2);
								}
								
								selectedPlayer.setDnp(false);
								selectedPlayer.setScoreUsed(false);
								
								loggerUtils.log("info", "Adding player as emergency to selected team: player={}", selectedPlayer);
								tmpSelectedTeam.add(selectedPlayer);
							}
						}
												
						if(earlyGamesCompleted) {
							TeamInsOutsLoaderHandler selectionsLoader = new TeamInsOutsLoaderHandler();
							selectionsLoader.configureLogging(mdcKey, loggerName, logfile);
							
							selectionsLoader.execute(validationResult.getTeamCode(), validationResult.getRound(), validationResult.getInsAndOuts().get("in"),
									                 validationResult.getInsAndOuts().get("out"), validationResult.getEmergencies(), false);	
						}
						
					} else {
						loggerUtils.log("info", "Early Ins and Outs are invalid");
						emailValidationError(round, team, validationResult);
					}
				}
			} else {
				if(insAndOuts != null && insAndOuts.size() > 0) {
					List<DflSelectedPlayer> oldEmergencies = new ArrayList<>();
					
					loggerUtils.log("info", "Removing previous emergencies");
					for(DflSelectedPlayer player : tmpSelectedTeam) {
						if(player.isEmergency() != 0) {
							loggerUtils.log("info", "Previous emergency seletecPlayer={}", player);
							oldEmergencies.add(player);
						}
					}
					tmpSelectedTeam.removeAll(oldEmergencies);
					
					for(InsAndOuts inOrOut : insAndOuts) {
						if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN)) {
							DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), inOrOut.getTeamPlayerId());
							
							DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
							selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
							selectedPlayer.setRound(round);
							selectedPlayer.setTeamCode(team.getTeamCode());
							selectedPlayer.setTeamPlayerId(inOrOut.getTeamPlayerId());
							selectedPlayer.setDnp(false);
							selectedPlayer.setEmergency(0);
							selectedPlayer.setScoreUsed(true);
							
							loggerUtils.log("info", "Adding player to selected team: player={}", selectedPlayer);
							tmpSelectedTeam.add(selectedPlayer);
						} else if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT)) {
							DflSelectedPlayer droppedPlayer = null;
							for(DflSelectedPlayer selectedPlayer : tmpSelectedTeam) {
								if(inOrOut.getTeamPlayerId() == selectedPlayer.getTeamPlayerId()) {
									droppedPlayer = selectedPlayer;
									loggerUtils.log("info", "Dropping player from selected team: player={}", droppedPlayer);
									break;
								}
							}
							tmpSelectedTeam.remove(droppedPlayer);
						} else {
							DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), inOrOut.getTeamPlayerId());
							
							DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
							selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
							selectedPlayer.setRound(round);
							selectedPlayer.setTeamCode(team.getTeamCode());
							selectedPlayer.setTeamPlayerId(inOrOut.getTeamPlayerId());
							
							if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1)) {
								selectedPlayer.setEmergency(1);
							} else {
								selectedPlayer.setEmergency(2);
							}
							
							selectedPlayer.setDnp(false);
							selectedPlayer.setScoreUsed(false);
							
							loggerUtils.log("info", "Adding player as emergency to selected team: player={}", selectedPlayer);
							tmpSelectedTeam.add(selectedPlayer);
						}
					}
				}
			}
		
			List<DflSelectedPlayer> selectedTeam = new ArrayList<>();
			List<Integer> selectedPlayerIds = new ArrayList<>();
			
			for(DflSelectedPlayer tmpSelectedPlayer : tmpSelectedTeam) {
				if(selectedPlayerIds.contains(tmpSelectedPlayer.getPlayerId())) {
					loggerUtils.log("info", "Duplicate selected player: player={}", tmpSelectedPlayer);
				} else {
					DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
					selectedPlayer.setPlayerId(tmpSelectedPlayer.getPlayerId());
					selectedPlayer.setRound(round);
					selectedPlayer.setTeamCode(team.getTeamCode());
					selectedPlayer.setTeamPlayerId(tmpSelectedPlayer.getTeamPlayerId());
					selectedPlayer.setDnp(tmpSelectedPlayer.isDnp());
					selectedPlayer.setEmergency(tmpSelectedPlayer.isEmergency());
					selectedPlayer.setScoreUsed(tmpSelectedPlayer.isScoreUsed());
					
					selectedTeam.add(selectedPlayer);
					selectedPlayerIds.add(tmpSelectedPlayer.getPlayerId());
				}
			}
			
			loggerUtils.log("info", "Saving selected to DB: selected team={}", selectedTeam);
			dflSelectedTeamService.replaceTeamForRound(round, team.getTeamCode(), selectedTeam);
		}
	}
	
	private void emailValidationError(int round, DflTeam team, SelectedTeamValidation validationResult) throws Exception {
		
		String dflMngrEmail = globalsService.getEmailConfig().get("dflmngrEmailAddr");
		
		String subject = "Team Validation FAILURE";
		
		String messageBody = "Coach,\n\n" +
				 		     "Your selections were entered before some early games and were not fully validated.  The full validation has failed .... The reasons for this are:\n";

		if(validationResult.selectionFileMissing) {
			messageBody = messageBody + "\t- You sent the email with no selections.txt\n";
		} else if(validationResult.roundCompleted) {
			messageBody = messageBody + "\t- The round you have in your selections.txt has past\n";
		} else if(validationResult.lockedOut) {
			messageBody = messageBody + "\t- The round you have in your selections.txt is in progress and doesn't allow more selections\n";
		} else if(validationResult.unknownError) {
			messageBody = messageBody + "\t- Some exception occured follow up with email to xdfl google group.\n";
		} else if(!validationResult.teamPlayerCheckOk) {
			messageBody = messageBody + "\t- The ins and/or outs numbers sent are not correct\n";
		} else {
			if(!validationResult.ffCheckOk) {
				messageBody = messageBody + "\t- You have too many Full Forwards\n";
			}
			if(!validationResult.fwdCheckOk) {
				messageBody = messageBody + "\t- You have too many Forwards\n";
			}
			if(!validationResult.rckCheckOk) {
				messageBody = messageBody + "\t- You have too many Rucks\n";
			}
			if(!validationResult.midCheckOk) {
				messageBody = messageBody + "\t- You have too many Midfielders\n";
			}
			if(!validationResult.fbCheckOk) {
				messageBody = messageBody + "\t- You have too many Full Backs\n";
			}
			if(!validationResult.defCheckOk) {
				messageBody = messageBody + "\t- You have too many Defenders\n";
			}
			if(!validationResult.benchCheckOk) {
				messageBody = messageBody + "\t- You have too many on the bench.\n";
			}
		}
		
		messageBody = messageBody + "\nThere is nothing you can do from here, but if you belive this is an error please send an email to the google group.\n\n" +
				      "DFL Manager Admin";
		
		List<String> to = new ArrayList<>();
		
		if(this.emailOverride != null && !this.emailOverride.equals("")) {
			to.add(this.emailOverride);
		} else {
			to.add(team.getCoachEmail());
		}
				
		loggerUtils.log("info", "Emailing validation errors to={}; validationResult={}", to, validationResult);
		EmailUtils.sendTextEmail(to, dflMngrEmail, subject, messageBody, null);
	}
	
	private void sendFirstGameEmail(int round, String emailOveride) throws Exception {
		
		String dflMngrEmail = globalsService.getEmailConfig().get("dflmngrEmailAddr");
		
		String subject = "DFL Manager - Early Games";
		
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
		
		body = body + "<p>DFL Manager Admin</p>\n";
		body = body + "</body>\n</html>";
		
		List<DflTeam> teams = dflTeamService.findAll();
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
				
				//JndiProvider.bind();
				
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
