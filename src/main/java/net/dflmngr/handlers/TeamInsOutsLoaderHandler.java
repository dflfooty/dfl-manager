package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.DomainDecodes;
import net.dflmngr.model.entity.DflEarlyInsAndOuts;
import net.dflmngr.model.entity.DflSelectedPlayer;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.service.DflEarlyInsAndOutsService;
import net.dflmngr.model.service.DflSelectedTeamService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.InsAndOutsService;
import net.dflmngr.model.service.impl.DflEarlyInsAndOutsServiceImpl;
import net.dflmngr.model.service.impl.DflSelectedTeamServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.InsAndOutsServiceImpl;

public class TeamInsOutsLoaderHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "Selections";
	
	String mdcKey;
	String loggerName;
	String logfile;
		
	public TeamInsOutsLoaderHandler() throws Exception {		
		isExecutable = false;
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(String teamCode, int round, List<Integer> ins, List<Integer> outs, List<Double> emgs, boolean earlyGames) {
		
		try {
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "Processing ins and out selections for: teamCode={}; round={}; ins={}; outs={}; emergencies={}", teamCode, round, ins, outs, emgs);
			
			if(earlyGames) {
				loggerUtils.log("info", "Early Games, saving to early games ins and outs");
				
				DflEarlyInsAndOutsService dflEarlyInsAndOutsService = new DflEarlyInsAndOutsServiceImpl();
				List<DflEarlyInsAndOuts> earlyInsAndOuts = new ArrayList<>();
				
				for(Integer i : ins) {
					DflEarlyInsAndOuts in = new DflEarlyInsAndOuts();
					in.setRound(round);
					in.setTeamCode(teamCode);
					in.setTeamPlayerId(i);
					in.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN);
					
					earlyInsAndOuts.add(in);
				}
				
				for(Integer o : outs) {
					DflEarlyInsAndOuts out = new DflEarlyInsAndOuts();
					out.setRound(round);
					out.setTeamCode(teamCode);
					out.setTeamPlayerId(o);
					out.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT);
					
					earlyInsAndOuts.add(out);
				}
				
				for(double e : emgs) {
					DflEarlyInsAndOuts emg = new DflEarlyInsAndOuts();
					emg.setRound(round);
					emg.setTeamCode(teamCode);
					
					int eid = (int) e;
					emg.setTeamPlayerId(eid);
					
					int e1e2 = Integer.parseInt(Double.toString(e).split("\\.")[1].substring(0, 1));
					//double e1e2 = Math.floor((e - eid) * 100) / 100;
					//if(e1e2 == 0.1) {
					if(e1e2 == 1) {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1);
					} else {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG2);
					}
					
					earlyInsAndOuts.add(emg);
				}
				
				loggerUtils.log("info", "Saving early ins and outs to database: ", earlyInsAndOuts);
				dflEarlyInsAndOutsService.saveTeamInsAndOuts(earlyInsAndOuts);
				loggerUtils.log("info", "Ins and outs saved");
				
			} else {
				loggerUtils.log("info", "Not early games, saving to regular ins and outs");
				
				InsAndOutsService insAndOutsService = new InsAndOutsServiceImpl();
				List<InsAndOuts> insAndOuts = new ArrayList<>();
				
				for(Integer i : ins) {
					InsAndOuts in = new InsAndOuts();
					in.setRound(round);
					in.setTeamCode(teamCode);
					in.setTeamPlayerId(i);
					in.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN);
					
					insAndOuts.add(in);
				}
				
				for(Integer o : outs) {
					InsAndOuts out = new InsAndOuts();
					out.setRound(round);
					out.setTeamCode(teamCode);
					out.setTeamPlayerId(o);
					out.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT);
					
					insAndOuts.add(out);
				}
				
				for(double e : emgs) {
					InsAndOuts emg = new InsAndOuts();
					emg.setRound(round);
					emg.setTeamCode(teamCode);
					
					int eid = (int) e;
					emg.setTeamPlayerId(eid);
					
					
					int e1e2 = Integer.parseInt(Double.toString(e).split("\\.")[1].substring(0, 1));
					//double e1e2 = Math.floor((e - eid) * 100) / 100;
					//if(e1e2 == 0.1) {
					if(e1e2 == 1) {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1);
					} else {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG2);
					}
					
					insAndOuts.add(emg);
				}
				
				loggerUtils.log("info", "Saving ins and outs to database: ", insAndOuts);
				insAndOutsService.saveTeamInsAndOuts(insAndOuts);
				loggerUtils.log("info", "Ins and outs saved");
				
				createTeamSelections(round, teamCode, insAndOuts);
								
				insAndOutsService.close();
			}
						
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private void createTeamSelections(int round, String teamCode, List<InsAndOuts> insAndOuts) throws Exception {

		loggerUtils.log("info", "Creating team selections");		
		loggerUtils.log("info", "Working with team={}", teamCode);
		
		DflSelectedTeamService dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		DflTeamPlayerService dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		
		List<DflSelectedPlayer> tmpSelectedTeam = null;
		List<DflSelectedPlayer> previousSelectedTeam = null;
			
		if(round == 1) {
			tmpSelectedTeam = new ArrayList<>();
			loggerUtils.log("info", "Round 1: no previous team");
		} else {
			previousSelectedTeam = dflSelectedTeamService.getSelectedTeamForRound(round-1, teamCode);
			tmpSelectedTeam = previousSelectedTeam;
			loggerUtils.log("info", "Not round 1: previous team: {}", tmpSelectedTeam);
		}
						
		loggerUtils.log("info", "Final ins and outs: {}", insAndOuts);
			
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
					DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(teamCode, inOrOut.getTeamPlayerId());
							
					DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
					selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
					selectedPlayer.setRound(round);
					selectedPlayer.setTeamCode(teamCode);
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
					DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(teamCode, inOrOut.getTeamPlayerId());
							
					DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
					selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
					selectedPlayer.setRound(round);
					selectedPlayer.setTeamCode(teamCode);
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
		
			List<DflSelectedPlayer> selectedTeam = new ArrayList<>();
			List<Integer> selectedPlayerIds = new ArrayList<>();
			
			Map<Integer, DflSelectedPlayer> previousSelectedTeamMap = previousSelectedTeam.stream().collect(Collectors.toMap(player -> player.getPlayerId(), player -> player));
			
			for(DflSelectedPlayer tmpSelectedPlayer : tmpSelectedTeam) {
				if(selectedPlayerIds.contains(tmpSelectedPlayer.getPlayerId())) {
					loggerUtils.log("info", "Duplicate selected player: player={}", tmpSelectedPlayer);
				} else {
					DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
					selectedPlayer.setPlayerId(tmpSelectedPlayer.getPlayerId());
					selectedPlayer.setRound(round);
					selectedPlayer.setTeamCode(teamCode);
					selectedPlayer.setTeamPlayerId(tmpSelectedPlayer.getTeamPlayerId());
					selectedPlayer.setEmergency(tmpSelectedPlayer.isEmergency());
					
					if(previousSelectedTeamMap.containsKey(tmpSelectedPlayer.getPlayerId())) {
						DflSelectedPlayer previousSelectedPlayer = previousSelectedTeamMap.get(tmpSelectedPlayer.getPlayerId());
						selectedPlayer.setDnp(previousSelectedPlayer.isDnp());
						selectedPlayer.setScoreUsed(previousSelectedPlayer.isScoreUsed());
						selectedPlayer.setHasPlayed(previousSelectedPlayer.hasPlayed());
						selectedPlayer.setReplacementInd(previousSelectedPlayer.getReplacementInd());
					} else {
						selectedPlayer.setDnp(false);

						if(tmpSelectedPlayer.isEmergency() != 0) {
							selectedPlayer.setScoreUsed(false);
						} else {
							selectedPlayer.setScoreUsed(true);
						}
					}
					
					selectedTeam.add(selectedPlayer);
					selectedPlayerIds.add(tmpSelectedPlayer.getPlayerId());
				}
			}
						
			loggerUtils.log("info", "Saving selected to DB: selected team={}", selectedTeam);
			dflSelectedTeamService.replaceTeamForRound(round, teamCode, selectedTeam);
			
			loggerUtils.log("info", "Creating predictions");
			PredictionHandler predictions = new PredictionHandler();
			predictions.configureLogging(mdcKey, loggerName, logfile);
			predictions.execute(round, teamCode, false);
		}
	}
}