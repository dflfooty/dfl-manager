package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.dflmngr.exceptions.UnknownPositionException;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.entity.DflSelectedPlayer;
import net.dflmngr.model.entity.DflSelectionIds;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflEarlyInsAndOutsService;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.DflSelectedTeamService;
import net.dflmngr.model.service.DflSelectionIdsService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflEarlyInsAndOutsServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.DflSelectedTeamServiceImpl;
import net.dflmngr.model.service.impl.DflSelectionIdsServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.validation.SelectedTeamValidation;

public class SelectedTeamValidationHandler {
	private LoggingUtils loggerUtils;

	boolean isExecutable;

	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "SelectedTeamValidationHandler";

	String mdcKey;
	String loggerName;
	String logfile;

	private DflSelectedTeamService dflSelectedTeamService;
	private DflTeamPlayerService dflTeamPlayerService;
	private DflPlayerService dflPlayerService;
	private GlobalsService globalsService;
	private DflRoundInfoService dflRoundInfoService;
	private DflEarlyInsAndOutsService dflEarlyInsAndOutsService;
	private AflFixtureService aflFixtureService;
	private DflSelectionIdsService dflSelectionIdsService;

	public SelectedTeamValidationHandler() {
		dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		dflPlayerService = new DflPlayerServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflRoundInfoService = new DflRoundInfoServiceImpl();
		dflEarlyInsAndOutsService = new DflEarlyInsAndOutsServiceImpl();
		aflFixtureService = new AflFixtureServiceImpl();
		dflSelectionIdsService = new DflSelectionIdsServiceImpl();
		isExecutable = false;
	}

	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}

	public SelectedTeamValidation execute(int round, String teamCode, Map<String, List<Integer>> insAndOuts, List<Double> emergencies, String selectionId) {

		SelectedTeamValidation validationResult = null;

		try {
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			loggerUtils.log("info", "Validating selectionds for teamCode={}; round={};", teamCode, round);

			int currentRound = Integer.parseInt(globalsService.getCurrentRound());

			validationResult = standardValidation(round, currentRound, teamCode, insAndOuts, emergencies, selectionId);
			validationResult.setRound(round);
			validationResult.setTeamCode(teamCode);

			if(validationResult.getInsAndOuts() == null) {
				validationResult.setInsAndOuts(insAndOuts);
			}
			if(validationResult.getEmergencies() == null) {
				validationResult.setEmergencies(emergencies);
			}

			loggerUtils.log("info", "Validation result={}", validationResult);

			dflSelectedTeamService.close();
			dflTeamPlayerService.close();
			dflPlayerService.close();
			globalsService.close();
			dflRoundInfoService.close();
			dflEarlyInsAndOutsService.close();
			aflFixtureService.close();

		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}

		return validationResult;
	}

	private SelectedTeamValidation standardValidation(int round, int currentRound, String teamCode, Map<String, List<Integer>> insAndOuts, List<Double> emergencies, String selectionId) {

		SelectedTeamValidation validationResult = new SelectedTeamValidation();

		loggerUtils.log("info", "DFL round={};", currentRound);

		List<DflSelectedPlayer> selectedTeam = null;

		validationResult.selectedWarning = false;
		validationResult.droppedWarning = false;

		List<Integer> checkedIns = new ArrayList<>();
		List<Integer> checkedOuts = new ArrayList<>();
		List<Double> checkedEmgs = new ArrayList<>();

		List<DflPlayer> selectedWarnPlayers = new ArrayList<>();
		List<DflPlayer> droppedWarnPlayers = new ArrayList<>();

		List<DflPlayer> dupInPlayers = new ArrayList<>();
		List<DflPlayer> dupOutPlayers = new ArrayList<>();
		List<DflPlayer> dupEmgPlayers = new ArrayList<>();

		DflRoundInfo roundInfo = dflRoundInfoService.get(round);
		List<Integer> aflRounds = new ArrayList<>();

		for(DflRoundMapping mapping : roundInfo.getRoundMapping()) {
			if(!aflRounds.contains(mapping.getAflRound())) {
				aflRounds.add(mapping.getAflRound());
			}
		}

		boolean aflRoundComplete = true;
		for(int aflRound : aflRounds) {
			aflRoundComplete = aflRoundComplete && aflFixtureService.getAflRoundComplete(aflRound);
		}

		boolean selectionExists = dflSelectionIdsService.selectionIdExists(round, teamCode, selectionId);

		if(round < currentRound) {
			validationResult.selectionFileMissing = false;
			validationResult.roundCompleted = true;
			loggerUtils.log("info", "Team invalid round is completed");
		} else if(aflRoundComplete) {
			validationResult.selectionFileMissing = false;
			validationResult.roundCompleted = false;
			validationResult.lockedOut = true;
			loggerUtils.log("info", "Team invalid email recived after AFL round complete");
		} else if(selectionExists) {
			validationResult.selectionFileMissing = false;
			validationResult.roundCompleted = false;
			validationResult.lockedOut = false;
			validationResult.duplicateSubmissionId = true;
			loggerUtils.log("info", "Already handled a selection for round={}. teamCode={}, id={}", round, teamCode, selectionId);
		} else {
			if(round == 1) {
				loggerUtils.log("info", "Round 1 only ins.");
				List<Integer> ins = insAndOuts.get("in");

				selectedTeam = new ArrayList<>();

				for(int in : ins) {
					if(checkedIns.contains(in)) {
						loggerUtils.log("info", "Duplicates ins, not included in={}.", in);
						validationResult.duplicateIns = true;
						DflPlayer player = dflPlayerService.get(in);
						dupInPlayers.add(player);
					} else {
						if(in < 1 || in > 45) {
							validationResult.selectionFileMissing = false;
							validationResult.roundCompleted = false;
							validationResult.lockedOut = false;
							loggerUtils.log("info", "Selected player outside player range, teamPlayerId={}.", in);
							break;
						} else {
							DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();

							selectedPlayer.setRound(round);
							selectedPlayer.setTeamCode(teamCode);
							selectedPlayer.setTeamPlayerId(in);
							selectedPlayer.setEmergency(0);
							selectedPlayer.setDnp(false);

							selectedTeam.add(selectedPlayer);
							loggerUtils.log("info", "Added selectedPlayer={}.", selectedPlayer);
						}

						checkedIns.add(in);
					}
				}

				for(Double emg : emergencies) {
					int emergency = emg.intValue();

					if(checkedEmgs.contains(emg) || checkedIns.contains(emergency)) {
						loggerUtils.log("info", "Duplicate emgergency, not included in={}.", emergency);
						validationResult.duplicateEmgs = true;
						DflPlayer player = dflPlayerService.get(emergency);
						dupEmgPlayers.add(player);
					} else {
						if(emergency < 1 || emergency > 45) {
							validationResult.selectionFileMissing = false;
							validationResult.roundCompleted = false;
							validationResult.lockedOut = false;
							loggerUtils.log("info", "Emergency player outside player range, teamPlayerId={}.", emergency);
							break;
						} else {
							DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();

							selectedPlayer.setRound(round);
							selectedPlayer.setTeamCode(teamCode);
							selectedPlayer.setTeamPlayerId(emergency);

							int e1e2 = Integer.parseInt(Double.toString(emg).split("\\.")[1].substring(0, 1));
							if(e1e2 == 1) {
								selectedPlayer.setEmergency(1);
							} else {
								selectedPlayer.setEmergency(2);
							}

							selectedPlayer.setDnp(false);

							selectedTeam.add(selectedPlayer);
							loggerUtils.log("info", "Added selectedPlayer={}, as emergency", selectedPlayer);
						}

						checkedEmgs.add(emg);
					}
				}
			} else {
				selectedTeam = dflSelectedTeamService.getSelectedTeamForRound(round-1, teamCode);

				loggerUtils.log("info", "Removeing emergencies from previous round");
				List<DflSelectedPlayer> playersToRemove = new ArrayList<>();
				for(DflSelectedPlayer selectedPlayer : selectedTeam) {
					if(selectedPlayer.isEmergency() != 0) {
						playersToRemove.add(selectedPlayer);
						loggerUtils.log("info", "Removing emergency={}", selectedPlayer);
					}
				}
				selectedTeam.removeAll(playersToRemove);
				playersToRemove.clear();

				List<Integer> ins = insAndOuts.get("in");
				List<Integer> outs = insAndOuts.get("out");

				for(int in : ins) {
					if(checkedIns.contains(in)) {
						loggerUtils.log("info", "Duplicates ins, not included in={}.", in);
						validationResult.duplicateIns = true;
						DflPlayer player = dflPlayerService.get(in);
						dupInPlayers.add(player);
					} else {
						if(in < 1 || in > 45) {
							validationResult.selectionFileMissing = false;
							loggerUtils.log("info", "Selected player outside player range, teamPlayerId={}.", in);
							break;
						} else {
							boolean found = false;
							boolean isEmg = false;
							for(DflSelectedPlayer selectedPlayer : selectedTeam) {
								if(in == selectedPlayer.getTeamPlayerId()) {
									found = true;
									if(selectedPlayer.isEmergency() == 1 || selectedPlayer.isEmergency() == 2) {
										isEmg = true;
									}
									break;
								}
							}
							if(!found) {
								DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();

								selectedPlayer.setRound(round);
								selectedPlayer.setTeamCode(teamCode);
								selectedPlayer.setTeamPlayerId(in);
								selectedPlayer.setEmergency(0);
								selectedPlayer.setDnp(false);

								selectedTeam.add(selectedPlayer);
								loggerUtils.log("info", "Added selectedPlayer={}.", selectedPlayer);
							} else {
								if(!isEmg) {
									loggerUtils.log("info", "Player already selected, teamPlayerId={}.", in);
									DflPlayer player = dflPlayerService.get(in);
									selectedWarnPlayers.add(player);
									validationResult.selectedWarning = true;
								}
							}
						}

						checkedIns.add(in);
					}
				}

				for(int out : outs) {
					if(checkedOuts.contains(out)) {
						loggerUtils.log("info", "Duplicates out, not included in={}.", out);
						validationResult.duplicateOuts = true;
						DflPlayer player = dflPlayerService.get(out);
						dupOutPlayers.add(player);
					} else {
						if(out < 1 || out > 45) {
							validationResult = new SelectedTeamValidation();
							validationResult.selectionFileMissing = false;
							loggerUtils.log("info", "Dropped player outside player range, teamPlayerId={}.", out);
							break;
						} else {
							boolean found = false;
							for(DflSelectedPlayer selectedPlayer : selectedTeam) {
								if(selectedPlayer.getTeamPlayerId() == out) {
									playersToRemove.add(selectedPlayer);
									found = true;
									loggerUtils.log("info", "Removing selectedPlayer={}.", selectedPlayer);
								}
							}
							if(!found) {
								loggerUtils.log("info", "Dropped player not selected, teamPlayerId={}.", out);
								DflPlayer player = dflPlayerService.get(out);
								droppedWarnPlayers.add(player);
								validationResult.droppedWarning = true;
							}
						}

						checkedOuts.add(out);
					}
				}

				for(Double emg : emergencies) {
					int emergency = emg.intValue();

					if(checkedEmgs.contains(emg) || checkedIns.contains(emergency)) {
						loggerUtils.log("info", "Duplicate emgergency, not included emg={}.", emergency);
						validationResult.duplicateEmgs = true;
						DflPlayer player = dflPlayerService.get(emergency);
						dupEmgPlayers.add(player);
					} else {
						if(emergency < 1 || emergency > 45) {
							validationResult = new SelectedTeamValidation();
							validationResult.selectionFileMissing = false;
							loggerUtils.log("info", "Emergency player outside player range, teamPlayerId={}.", emergency);
							break;
						} else {
							boolean alreadySelected = false;
							for(DflSelectedPlayer selectedPlayer : selectedTeam) {
								if(emergency == selectedPlayer.getPlayerId()) {
									alreadySelected = true;
									break;
								}
							}
							if(alreadySelected) {
								loggerUtils.log("info", "Emergency emg={}.", emergency);
							} else {
								DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();

								selectedPlayer.setRound(round);
								selectedPlayer.setTeamCode(teamCode);
								selectedPlayer.setTeamPlayerId(emergency);

								int e1e2 = Integer.parseInt(Double.toString(emg).split("\\.")[1].substring(0, 1));
								if(e1e2 == 1) {
									selectedPlayer.setEmergency(1);
								} else {
									selectedPlayer.setEmergency(2);
								}

								selectedPlayer.setDnp(false);

								selectedTeam.add(selectedPlayer);
								loggerUtils.log("info", "Added selectedPlayer={}.", selectedPlayer);
							}
						}

						checkedEmgs.add(emg);
					}
				}

				selectedTeam.removeAll(playersToRemove);
			}

			loggerUtils.log("info", "Pre checks PASSED, validating selected team");

			insAndOuts.replace("in", checkedIns);
			insAndOuts.replace("out", checkedOuts);

			validationResult.setInsAndOuts(insAndOuts);
			validationResult.setEmergencies(checkedEmgs);

			if(validationResult.selectedWarning) {
				validationResult.selectedWarnPlayers = selectedWarnPlayers;
			}
			if(validationResult.selectedWarning) {
				validationResult.droppedWarnPlayers = droppedWarnPlayers;
			}
			if(validationResult.duplicateIns) {
				validationResult.dupInPlayers = dupInPlayers;
			}
			if(validationResult.duplicateOuts) {
				validationResult.dupOutPlayers = dupOutPlayers;
			}
			if(validationResult.duplicateEmgs) {
				validationResult.dupEmgPlayers = dupEmgPlayers;
			}

			validationResult = validateTeam(selectedTeam, validationResult);

			if(!selectionId.equalsIgnoreCase("noid")) {
				DflSelectionIds selectionIds = new DflSelectionIds();
				selectionIds.setRound(round);
				selectionIds.setTeamCode(teamCode);
				selectionIds.setSelectionId(selectionId);

				dflSelectionIdsService.insert(selectionIds);
			}
		}

		return validationResult;
	}

	private SelectedTeamValidation validateTeam(List<DflSelectedPlayer> selectedTeam, SelectedTeamValidation validationResult) {

		validationResult.selectionFileMissing = false;
		validationResult.roundCompleted = false;
		validationResult.lockedOut = false;
		validationResult.teamPlayerCheckOk = true;

		int ffCount = 0;
		int fwdCount = 0;
		int midCount = 0;
		int defCount = 0;
		int fbCount = 0;
		int rckCount = 0;
		int benchCount = 0;

		List<DflPlayer> ffPlayers = new ArrayList<>();
		List<DflPlayer> fwdPlayers = new ArrayList<>();
		List<DflPlayer> midPlayers = new ArrayList<>();
		List<DflPlayer> defPlayers = new ArrayList<>();
		List<DflPlayer> fbPlayers = new ArrayList<>();
		List<DflPlayer> rckPlayers = new ArrayList<>();

		List<String> emergencyPositions = new ArrayList<>();
		List<DflPlayer> emgPlayers = new ArrayList<>();

		List<DflPlayer> emgFfPlayers = new ArrayList<>();
		List<DflPlayer> emgFwdPlayers = new ArrayList<>();
		List<DflPlayer> emgMidPlayers = new ArrayList<>();
		List<DflPlayer> emgDefPlayers = new ArrayList<>();
		List<DflPlayer> emgFbPlayers = new ArrayList<>();
		List<DflPlayer> emgRckPlayers = new ArrayList<>();

		for(DflSelectedPlayer selectedPlayer : selectedTeam) {
			DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(selectedPlayer.getTeamCode(), selectedPlayer.getTeamPlayerId());
			DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());

			String position = player.getPosition().toLowerCase();

			if(selectedPlayer.isEmergency() == 0) {
				switch(position) {
					case "ff" :
						ffCount++;
						ffPlayers.add(player);
						break;
					case "fwd" :
						fwdCount++;
						fwdPlayers.add(player);
						break;
					case "rck" :
						rckCount++;
						rckPlayers.add(player);
						break;
					case "mid" :
						midCount++;
						midPlayers.add(player);
						break;
					case "def" :
						defCount++;
						defPlayers.add(player);
						break;
					case "fb" :
						fbCount++;
						fbPlayers.add(player);
						break;
					default: throw new UnknownPositionException(position);
				}
			} else {
				emergencyPositions.add(position);
				emgPlayers.add(player);
			}
		}

		loggerUtils.log("info", "Position counts: ffCount={}; fwdCount={}; midCount={}; defCount={}; fbCount={}; rckCount={}, emergencyPositions={};",
						ffCount, fwdCount, midCount, defCount, fbCount, rckCount, emergencyPositions);

		if(ffCount <= 2) {
			validationResult.ffCheckOk = true;
			validationResult.ffPlayers = ffPlayers;
		}
		if(fwdCount <= 6) {
			validationResult.fwdCheckOk = true;
			validationResult.fwdPlayers = fwdPlayers;
		}
		if(midCount <= 6) {
			validationResult.midCheckOk = true;
			validationResult.midPlayers = midPlayers;
		}
		if(defCount <= 6) {
			validationResult.defCheckOk = true;
			validationResult.defPlayers = defPlayers;
		}
		if(fbCount <= 2) {
			validationResult.fbCheckOk = true;
			validationResult.fbPlayers = fbPlayers;
		}
		if(rckCount <= 2) {
			validationResult.rckCheckOk = true;
			validationResult.rckPlayers = rckPlayers;
		}

		if(ffCount == 2) {
			benchCount++;
		}
		if(fwdCount == 6) {
			benchCount++;
		}
		if(midCount == 6) {
			benchCount++;
		}
		if(defCount == 6) {
			benchCount++;
		}
		if(fbCount == 2) {
			benchCount++;
		}
		if(rckCount == 2) {
			benchCount++;
		}

		loggerUtils.log("info", "Bench count={};", benchCount);

		if(benchCount <= 4) {
			validationResult.benchCheckOk = true;
		}

		for(String position : emergencyPositions) {
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
				default: throw new UnknownPositionException(position);
			}
		}

		if(ffCount > 2 || fwdCount > 6 || midCount > 6 || defCount > 6 || fbCount > 2 || rckCount > 2) {
			List<Double> emgs = validationResult.getEmergencies();
			for(DflPlayer player : emgPlayers) {
				switch(player.getPosition().toLowerCase()) {
					case "ff" :
						if(ffCount > 2) {
							validationResult.emergencyFfWarning = true;
							emgFfPlayers.add(player);

							loggerUtils.log("info", "Invalid emergency, player={};", player);

							int emg1 = 0;
							int emg2 = 0;

							if(emgs.size() >= 1) {
								emg1 = emgs.get(0).intValue();
							}
							if(emgs.size() >= 2) {
								emg2 = emgs.get(1).intValue();
							}

							DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

							if(emg1 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(0);
								loggerUtils.log("info", "Removed emergency 1, player={};", teamPlayer);
							}
							if(emg2 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(1);
								loggerUtils.log("info", "Removed emergency 2, player={};", teamPlayer);
							}
						}
						break;
					case "fwd" :
						if(fwdCount > 6) {
							validationResult.emergencyFwdWarning = true;
							emgFwdPlayers.add(player);

							loggerUtils.log("info", "Invalid emergency, player={};", player);

							int emg1 = 0;
							int emg2 = 0;

							if(emgs.size() >= 1) {
								emg1 = emgs.get(0).intValue();
							}
							if(emgs.size() >= 2) {
								emg2 = emgs.get(1).intValue();
							}

							DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

							if(emg1 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(0);
								loggerUtils.log("info", "Removed emergency 1, player={};", teamPlayer);
							}
							if(emg2 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(1);
								loggerUtils.log("info", "Removed emergency 2, player={};", teamPlayer);
							}
						}
						break;
					case "rck" :
						if(rckCount > 2) {
							validationResult.emergencyRckWarning = true;
							emgRckPlayers.add(player);

							loggerUtils.log("info", "Invalid emergency, player={};", player);

							int emg1 = 0;
							int emg2 = 0;

							if(emgs.size() >= 1) {
								emg1 = emgs.get(0).intValue();
							}
							if(emgs.size() >= 2) {
								emg2 = emgs.get(1).intValue();
							}

							DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

							if(emg1 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(0);
								loggerUtils.log("info", "Removed emergency 1, player={};", teamPlayer);
							}
							if(emg2 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(1);
								loggerUtils.log("info", "Removed emergency 2, player={};", teamPlayer);
							}
						}
						break;
					case "mid" :
						if(midCount > 6) {
							validationResult.emergencyMidWarning = true;
							emgMidPlayers.add(player);

							loggerUtils.log("info", "Invalid emergency, player={};", player);

							int emg1 = 0;
							int emg2 = 0;

							if(emgs.size() >= 1) {
								emg1 = emgs.get(0).intValue();
							}
							if(emgs.size() >= 2) {
								emg2 = emgs.get(1).intValue();
							}

							DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

							if(emg1 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(0);
								loggerUtils.log("info", "Removed emergency 1, player={};", teamPlayer);
							}
							if(emg2 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(1);
								loggerUtils.log("info", "Removed emergency 2, player={};", teamPlayer);
							}
						}
						break;
					case "def" :
						if(defCount > 6) {
							validationResult.emergencyDefWarning = true;
							emgDefPlayers.add(player);

							loggerUtils.log("info", "Invalid emergency, player={};", player);

							int emg1 = 0;
							int emg2 = 0;

							if(emgs.size() >= 1) {
								emg1 = emgs.get(0).intValue();
							}
							if(emgs.size() >= 2) {
								emg2 = emgs.get(1).intValue();
							}

							DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

							if(emg1 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(0);
								loggerUtils.log("info", "Removed emergency 1, player={};", teamPlayer);
							}
							if(emg2 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(1);
								loggerUtils.log("info", "Removed emergency 2, player={};", teamPlayer);
							}
						}
						break;
					case "fb" :
						if(fbCount > 2) {
							validationResult.emergencyFbWarning = true;
							emgFbPlayers.add(player);

							loggerUtils.log("info", "Invalid emergency, player={};", player);

							int emg1 = 0;
							int emg2 = 0;

							if(emgs.size() >= 1) {
								emg1 = emgs.get(0).intValue();
							}
							if(emgs.size() >= 2) {
								emg2 = emgs.get(1).intValue();
							}

							DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

							if(emg1 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(0);
								loggerUtils.log("info", "Removed emergency 1, player={};", teamPlayer);
							}
							if(emg2 == teamPlayer.getTeamPlayerId()) {
								emgs.remove(1);
								loggerUtils.log("info", "Removed emergency 2, player={};", teamPlayer);
							}
						}
						break;
					default: throw new UnknownPositionException(player.getPosition());
				}

				validationResult.setEmergencies(emgs);
			}
		}

		if(validationResult.emergencyFfWarning) {
			validationResult.emgFfPlayers = emgFfPlayers;
		}
		if(validationResult.emergencyFwdWarning) {
			validationResult.emgFwdPlayers = emgFwdPlayers;
		}
		if(validationResult.emergencyRckWarning) {
			validationResult.emgRckPlayers = emgRckPlayers;
		}
		if(validationResult.emergencyMidWarning) {
			validationResult.emgMidPlayers = emgMidPlayers;
		}
		if(validationResult.emergencyDefWarning) {
			validationResult.emgDefPlayers = emgDefPlayers;
		}
		if(validationResult.emergencyFbWarning) {
			validationResult.emgFbPlayers = emgFbPlayers;
		}

		return validationResult;
	}
}
