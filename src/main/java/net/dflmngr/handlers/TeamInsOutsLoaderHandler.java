package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.HashMap;
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

	DflSelectedTeamService dflSelectedTeamService;
	DflTeamPlayerService dflTeamPlayerService;
		
	public TeamInsOutsLoaderHandler() {
		dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		
		isExecutable = false;
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
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
				handleEarlyGames(teamCode, round, ins, outs, emgs);
			} else {
				handleWithoutEarlyGames(teamCode, round, ins, outs, emgs);
			}

			dflSelectedTeamService.close();
			dflTeamPlayerService.close();
						
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}

	private void handleEarlyGames(String teamCode, int round, List<Integer> ins, List<Integer> outs, List<Double> emgs) {
		loggerUtils.log("info", "Early Games, saving to early games ins and outs");
				
		DflEarlyInsAndOutsService dflEarlyInsAndOutsService = new DflEarlyInsAndOutsServiceImpl();
		List<DflEarlyInsAndOuts> earlyInsAndOuts = new ArrayList<>();
		
		setEarlyIns(teamCode, round, ins);
		setEarlyOuts(teamCode, round, outs);
		setEarlyEmgs(teamCode, round, emgs);
		
		loggerUtils.log("info", "Saving early ins and outs to database: ", earlyInsAndOuts);
		dflEarlyInsAndOutsService.saveTeamInsAndOuts(earlyInsAndOuts);
		loggerUtils.log("info", "Ins and outs saved");
	}

	private void handleWithoutEarlyGames(String teamCode, int round, List<Integer> ins, List<Integer> outs, List<Double> emgs) {
		loggerUtils.log("info", "Not early games, saving to regular ins and outs");
				
		InsAndOutsService insAndOutsService = new InsAndOutsServiceImpl();
		List<InsAndOuts> insAndOuts = new ArrayList<>();
		
		insAndOuts.addAll(setIns(teamCode, round, ins));
		insAndOuts.addAll(setOuts(teamCode, round, outs));
		insAndOuts.addAll(setEmgs(teamCode, round, emgs));

		loggerUtils.log("info", "Saving ins and outs to database: ", insAndOuts);
		insAndOutsService.saveTeamInsAndOuts(insAndOuts);
		loggerUtils.log("info", "Ins and outs saved");
		
		createTeamSelections(round, teamCode, insAndOuts);
						
		insAndOutsService.close();
	}

	private List<DflEarlyInsAndOuts> setEarlyIns(String teamCode, int round, List<Integer> ins) {
		List<DflEarlyInsAndOuts> earlyIns = new ArrayList<>();

		for(Integer i : ins) {
			DflEarlyInsAndOuts in = new DflEarlyInsAndOuts();
			in.setRound(round);
			in.setTeamCode(teamCode);
			in.setTeamPlayerId(i);
			in.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN);
			
			earlyIns.add(in);
		}
		
		return earlyIns;
	}

	private List<DflEarlyInsAndOuts> setEarlyOuts(String teamCode, int round, List<Integer> outs) {
		List<DflEarlyInsAndOuts> earlyOuts = new ArrayList<>();

		for(Integer o : outs) {
			DflEarlyInsAndOuts out = new DflEarlyInsAndOuts();
			out.setRound(round);
			out.setTeamCode(teamCode);
			out.setTeamPlayerId(o);
			out.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT);
			
			earlyOuts.add(out);
		}

		return earlyOuts;
	}

	private List<DflEarlyInsAndOuts> setEarlyEmgs(String teamCode, int round, List<Double> emgs) {
		List<DflEarlyInsAndOuts> earlyEmgs = new ArrayList<>();

		for(Double e : emgs) {
			DflEarlyInsAndOuts emg = new DflEarlyInsAndOuts();
			emg.setRound(round);
			emg.setTeamCode(teamCode);
			
			int eid = e.intValue();
			emg.setTeamPlayerId(eid);
			
			int e1e2 = Integer.parseInt(Double.toString(e).split("\\.")[1].substring(0, 1));
			if(e1e2 == 1) {
				emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1);
			} else {
				emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG2);
			}
			
			earlyEmgs.add(emg);
		}

		return earlyEmgs;
	}

	private List<InsAndOuts> setIns(String teamCode, int round, List<Integer> ins) {
		List<InsAndOuts> fullIns = new ArrayList<>();

		for(Integer i : ins) {
			InsAndOuts in = new InsAndOuts();
			in.setRound(round);
			in.setTeamCode(teamCode);
			in.setTeamPlayerId(i);
			in.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN);
			
			fullIns.add(in);
		}

		return fullIns;
	}

	private List<InsAndOuts> setOuts(String teamCode, int round, List<Integer> outs) {
		List<InsAndOuts> fullOuts = new ArrayList<>();
		
		for(Integer o : outs) {
			InsAndOuts out = new InsAndOuts();
			out.setRound(round);
			out.setTeamCode(teamCode);
			out.setTeamPlayerId(o);
			out.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT);
			
			fullOuts.add(out);
		}

		return fullOuts;
	}

	private List<InsAndOuts> setEmgs(String teamCode, int round, List<Double> emgs) {
		List<InsAndOuts> fullEmgs = new ArrayList<>();

		for(Double e : emgs) {
			InsAndOuts emg = new InsAndOuts();
			emg.setRound(round);
			emg.setTeamCode(teamCode);
			
			int eid = e.intValue();
			emg.setTeamPlayerId(eid);
			
			
			int e1e2 = Integer.parseInt(Double.toString(e).split("\\.")[1].substring(0, 1));
			if(e1e2 == 1) {
				emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1);
			} else {
				emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG2);
			}
			
			fullEmgs.add(emg);
		}

		return fullEmgs;
	}
	
	private void createTeamSelections(int round, String teamCode, List<InsAndOuts> insAndOuts) {

		loggerUtils.log("info", "Creating team selections");		
		loggerUtils.log("info", "Working with team={}", teamCode);
				
		List<DflSelectedPlayer> tmpSelectedTeam = null;
			
		if(round == 1) {
			tmpSelectedTeam = new ArrayList<>();
			loggerUtils.log("info", "Round 1: no previous team");
		} else {
			tmpSelectedTeam = dflSelectedTeamService.getSelectedTeamForRound(round-1, teamCode);
			loggerUtils.log("info", "Not round 1: previous team: {}", tmpSelectedTeam);
		}
						
		loggerUtils.log("info", "Final ins and outs: {}", insAndOuts);
			
		if(insAndOuts != null && !insAndOuts.isEmpty()) {
			tmpSelectedTeam.removeAll(getOldEmergencies(tmpSelectedTeam));
			setInsAndOuts(round, teamCode, insAndOuts, tmpSelectedTeam);	
			selectTeam(round, teamCode, tmpSelectedTeam);
			createPredictions(round, teamCode);
		}
	}

	private List<DflSelectedPlayer> getOldEmergencies(List<DflSelectedPlayer> selectedTeam) {
		List<DflSelectedPlayer> oldEmergencies = new ArrayList<>();
					
		loggerUtils.log("info", "Removing previous emergencies");
		for(DflSelectedPlayer player : selectedTeam) {
			if(player.isEmergency() != 0) {
				loggerUtils.log("info", "Previous emergency seletecPlayer={}", player);
				oldEmergencies.add(player);
			}
		}

		return oldEmergencies;
	}

	private void setInsAndOuts(int round, String teamCode, List<InsAndOuts> insAndOuts, List<DflSelectedPlayer> selectedTeam) {
		for(InsAndOuts inOrOut : insAndOuts) {
			if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN)) {
				selectedTeam.add(selectIn(round, teamCode, inOrOut));
			} else if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT)) {
				selectedTeam.remove(selectOut(inOrOut, selectedTeam));
			} else {
				selectedTeam.add(selectEmg(round, teamCode, inOrOut));
			}
		}
	}

	private DflSelectedPlayer selectIn(int round, String teamCode, InsAndOuts inOrOut) {
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

		return selectedPlayer;
	}

	private DflSelectedPlayer selectOut(InsAndOuts inOrOut, List<DflSelectedPlayer> selectedTeam) {
		DflSelectedPlayer droppedPlayer = null;
		for(DflSelectedPlayer selectedPlayer : selectedTeam) {
			if(inOrOut.getTeamPlayerId() == selectedPlayer.getTeamPlayerId()) {
				droppedPlayer = selectedPlayer;
				loggerUtils.log("info", "Dropping player from selected team: player={}", droppedPlayer);
				break;
			}
		}

		return droppedPlayer;
	}

	private DflSelectedPlayer selectEmg(int round, String teamCode, InsAndOuts inOrOut) {
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

		return selectedPlayer;
	}

	private void selectTeam(int round, String teamCode, List<DflSelectedPlayer> tmpSelectedTeam) {
		List<DflSelectedPlayer> selectedTeam = new ArrayList<>();
		List<Integer> selectedPlayerIds = new ArrayList<>();
		
		List<DflSelectedPlayer> currentSelectedTeam = dflSelectedTeamService.getSelectedTeamForRound(round, teamCode);
		Map<Integer, DflSelectedPlayer> currentSelectedTeamMap = (currentSelectedTeam == null) 
		? new HashMap<>() :	currentSelectedTeam.stream().collect(Collectors.toMap(DflSelectedPlayer::getPlayerId, player -> player));
		
		for(DflSelectedPlayer tmpSelectedPlayer : tmpSelectedTeam) {
			if(selectedPlayerIds.contains(tmpSelectedPlayer.getPlayerId())) {
				loggerUtils.log("info", "Duplicate selected player: player={}", tmpSelectedPlayer);
			} else {				
				selectedTeam.add(setSelectedPlayer(round, teamCode, tmpSelectedPlayer, currentSelectedTeamMap));
				selectedPlayerIds.add(tmpSelectedPlayer.getPlayerId());
			}
		}
					
		loggerUtils.log("info", "Saving selected to DB: selected team={}", selectedTeam);
		dflSelectedTeamService.replaceTeamForRound(round, teamCode, selectedTeam);
	}

	private DflSelectedPlayer setSelectedPlayer(int round, String teamCode, DflSelectedPlayer tmpSelectedPlayer, Map<Integer, DflSelectedPlayer> currentSelectedTeamMap) {
		DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
		selectedPlayer.setPlayerId(tmpSelectedPlayer.getPlayerId());
		selectedPlayer.setRound(round);
		selectedPlayer.setTeamCode(teamCode);
		selectedPlayer.setTeamPlayerId(tmpSelectedPlayer.getTeamPlayerId());
		selectedPlayer.setEmergency(tmpSelectedPlayer.isEmergency());
		
		if(currentSelectedTeamMap.containsKey(tmpSelectedPlayer.getPlayerId())) {
			DflSelectedPlayer currentSelectedPlayer = currentSelectedTeamMap.get(tmpSelectedPlayer.getPlayerId());

			selectedPlayer.setHasPlayed(currentSelectedPlayer.hasPlayed());
			selectedPlayer.setDnp(currentSelectedPlayer.hasPlayed() ? currentSelectedPlayer.isDnp() : tmpSelectedPlayer.isDnp());
			selectedPlayer.setScoreUsed(currentSelectedPlayer.hasPlayed() ? currentSelectedPlayer.isScoreUsed() : tmpSelectedPlayer.isScoreUsed());
			selectedPlayer.setReplacementInd(currentSelectedPlayer.hasPlayed() ? currentSelectedPlayer.getReplacementInd() : tmpSelectedPlayer.getReplacementInd());
		} else {
			selectedPlayer.setDnp(false);
			selectedPlayer.setScoreUsed(tmpSelectedPlayer.isEmergency() == 0);
		}

		return selectedPlayer;
	}

	private void createPredictions(int round, String teamCode) {
		loggerUtils.log("info", "Creating predictions");
		PredictionHandler predictions = new PredictionHandler();
		predictions.configureLogging(mdcKey, loggerName, logfile);
		predictions.execute(round, teamCode, false);
	}
 }