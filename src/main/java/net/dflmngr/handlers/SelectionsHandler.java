package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.DomainDecodes;
import net.dflmngr.model.entity.DflSelectedPlayer;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.service.DflSelectedTeamService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.InsAndOutsService;
import net.dflmngr.model.service.impl.DflSelectedTeamServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.InsAndOutsServiceImpl;

public class SelectionsHandler {
	private LoggingUtils loggerUtils;

	boolean isExecutable;

	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "SelectionsHandler";

	String mdcKey;
	String loggerName;
	String logfile;

	DflTeamServiceImpl dflTeamService;
	InsAndOutsService insAndOutsService;
	DflSelectedTeamService dflSelectedTeamService;
	DflTeamPlayerService dflTeamPlayerService;

	public SelectionsHandler() {
		dflTeamService = new DflTeamServiceImpl();
		insAndOutsService = new InsAndOutsServiceImpl();
		dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
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

			loggerUtils.log("info", "Creating team selections");

			List<DflTeam> teams = dflTeamService.findAll();

			for(DflTeam team : teams) {
				List<InsAndOuts> insAndOuts = insAndOutsService.getByTeamAndRound(round, team.getTeamCode());
				createTeamSelections(round, team.getTeamCode(), insAndOuts);
			}

			dflTeamService.close();
			insAndOutsService.close();
			dflSelectedTeamService.close();
			dflTeamPlayerService.close();

			loggerUtils.log("info", "Team selections created");

		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}

	private void createTeamSelections(int round, String teamCode, List<InsAndOuts> insAndOuts) {

		loggerUtils.log("info", "Creating team selections");
		loggerUtils.log("info", "Working with team={}", teamCode);

		List<DflSelectedPlayer> selectedTeam = null;

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

		if(insAndOuts != null && !insAndOuts.isEmpty()) {
			selectedTeam = applyInsAndOuts(round, teamCode, tmpSelectedTeam, insAndOuts);
		} else {
			if(previousSelectedTeam != null) {
				selectedTeam = usePreviousTeam(round, teamCode, previousSelectedTeam);
			}
		}

		if(selectedTeam != null && !selectedTeam.isEmpty()) {
			loggerUtils.log("info", "Saving selected to DB: selected team={}", selectedTeam);
			dflSelectedTeamService.replaceTeamForRound(round, teamCode, selectedTeam);

			loggerUtils.log("info", "Creating predictions");
			PredictionHandler predictions = new PredictionHandler();
			predictions.configureLogging(mdcKey, loggerName, logfile);
			predictions.execute(round, teamCode, false);
		} else {
			loggerUtils.log("info", "Error Selecting team for round={} teamCode={}", round, teamCode);
		}
	}

	private List<DflSelectedPlayer> applyInsAndOuts(int round, String teamCode, List<DflSelectedPlayer> tmpSelectedTeam, List<InsAndOuts> insAndOuts) {
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
				tmpSelectedTeam.add(applyIn(round, teamCode, inOrOut.getTeamPlayerId()));
			} else if(inOrOut.getInOrOut().equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT)) {
				tmpSelectedTeam.remove(applyOut(tmpSelectedTeam, inOrOut.getTeamPlayerId()));
			} else {
				tmpSelectedTeam.add(applyEmg(round, teamCode, inOrOut.getTeamPlayerId(), inOrOut.getInOrOut()));
			}
		}

		return setSelectedPlayers(round, teamCode, tmpSelectedTeam);
	}

	private DflSelectedPlayer applyIn(int round, String teamCode, int teamPlayerId) {
		DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(teamCode, teamPlayerId);

		DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
		selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
		selectedPlayer.setRound(round);
		selectedPlayer.setTeamCode(teamCode);
		selectedPlayer.setTeamPlayerId(teamPlayerId);
		selectedPlayer.setDnp(false);
		selectedPlayer.setEmergency(0);
		selectedPlayer.setScoreUsed(true);

		loggerUtils.log("info", "Adding player to selected team: player={}", selectedPlayer);
		
		return selectedPlayer;
	}

	private DflSelectedPlayer applyOut(List<DflSelectedPlayer> tmpSelectedTeam, int teamPlayerId) {
		DflSelectedPlayer droppedPlayer = null;
		for(DflSelectedPlayer selectedPlayer : tmpSelectedTeam) {
			if(teamPlayerId == selectedPlayer.getTeamPlayerId()) {
				droppedPlayer = selectedPlayer;
				loggerUtils.log("info", "Dropping player from selected team: player={}", droppedPlayer);
				break;
			}
		}

		return droppedPlayer;
	}

	private DflSelectedPlayer applyEmg(int round, String teamCode, int teamPlayerId, String emgInd) {
		DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(teamCode, teamPlayerId);

		DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
		selectedPlayer.setPlayerId(teamPlayer.getPlayerId());
		selectedPlayer.setRound(round);
		selectedPlayer.setTeamCode(teamCode);
		selectedPlayer.setTeamPlayerId(teamPlayerId);

		if(emgInd.equals(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1)) {
			selectedPlayer.setEmergency(1);
		} else {
			selectedPlayer.setEmergency(2);
		}
		selectedPlayer.setDnp(false);
		selectedPlayer.setScoreUsed(false);

		loggerUtils.log("info", "Adding player as emergency to selected team: player={}", selectedPlayer);

		return selectedPlayer;
	}

	private List<DflSelectedPlayer> setSelectedPlayers(int round, String teamCode, List<DflSelectedPlayer> tmpSelectedTeam) {
		List<DflSelectedPlayer> selectedTeam = new ArrayList<>();
		List<Integer> selectedPlayerIds = new ArrayList<>();

		for(DflSelectedPlayer tmpSelectedPlayer : tmpSelectedTeam) {
			if(selectedPlayerIds.contains(tmpSelectedPlayer.getPlayerId())) {
				loggerUtils.log("info", "Duplicate selected player: player={}", tmpSelectedPlayer);
			} else {
				DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
				selectedPlayer.setPlayerId(tmpSelectedPlayer.getPlayerId());
				selectedPlayer.setRound(round);
				selectedPlayer.setTeamCode(teamCode);
				selectedPlayer.setTeamPlayerId(tmpSelectedPlayer.getTeamPlayerId());
				selectedPlayer.setDnp(false);
				selectedPlayer.setEmergency(tmpSelectedPlayer.isEmergency());
				selectedPlayer.setScoreUsed(tmpSelectedPlayer.isEmergency() == 0);
				selectedPlayer.setHasPlayed(false);
				selectedPlayer.setReplacementInd(null);

				selectedTeam.add(selectedPlayer);
				selectedPlayerIds.add(tmpSelectedPlayer.getPlayerId());
			}
		}

		return selectedTeam;
	}

	private List<DflSelectedPlayer> usePreviousTeam(int round, String teamCode, List<DflSelectedPlayer> previousSelectedTeam) {
		List<DflSelectedPlayer> selectedTeam = new ArrayList<>();

		for(DflSelectedPlayer prevSelectedPlayer : previousSelectedTeam) {
			DflSelectedPlayer selectedPlayer = new DflSelectedPlayer();
			selectedPlayer.setPlayerId(prevSelectedPlayer.getPlayerId());
			selectedPlayer.setRound(round);
			selectedPlayer.setTeamCode(teamCode);
			selectedPlayer.setTeamPlayerId(prevSelectedPlayer.getTeamPlayerId());
			selectedPlayer.setDnp(false);
			selectedPlayer.setEmergency(prevSelectedPlayer.isEmergency());
			selectedPlayer.setScoreUsed(prevSelectedPlayer.isEmergency() == 0);
			selectedPlayer.setHasPlayed(false);
			selectedPlayer.setReplacementInd(null);

			selectedTeam.add(selectedPlayer);
		}

		return selectedTeam;
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

			SelectionsHandler selectionsHandler = new SelectionsHandler();
			selectionsHandler.configureLogging("batch.name", "batch-logger", ("SelectionsHander" + round));
			selectionsHandler.execute(round);

			System.exit(0);

		} catch (ParseException ex) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "SelectionsHandler", options );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
