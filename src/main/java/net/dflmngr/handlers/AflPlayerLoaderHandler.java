package net.dflmngr.handlers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import net.dflmngr.jndi.JndiProvider;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflPlayer;
import net.dflmngr.model.entity.AflTeam;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflUnmatchedPlayer;
import net.dflmngr.model.service.AflPlayerService;
import net.dflmngr.model.service.AflTeamService;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.DflUnmatchedPlayerService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflPlayerServiceImpl;
import net.dflmngr.model.service.impl.AflTeamServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflUnmatchedPlayerServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflPlayerLoaderHandler {
	private LoggingUtils loggerUtils;

	private AflTeamService aflTeamService;
	private AflPlayerService aflPlayerService;
	private DflPlayerService dflPlayerService;
	private GlobalsService globalsService;
	private DflUnmatchedPlayerService dflUnmatchedPlayerService;

	private DateFormat df = new SimpleDateFormat("dd.MM.yy");

	public AflPlayerLoaderHandler() {

		//loggerUtils = new LoggingUtils("batch-logger", "batch.name", "AflPlayerLoader");
		loggerUtils = new LoggingUtils("AflPlayerLoader");

		try {

			//JndiProvider.bind();

			aflTeamService = new AflTeamServiceImpl();
			aflPlayerService = new AflPlayerServiceImpl();
			dflPlayerService = new DflPlayerServiceImpl();
			globalsService = new GlobalsServiceImpl();
			dflUnmatchedPlayerService = new DflUnmatchedPlayerServiceImpl();

		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}

	public void execute() {

		try {
			loggerUtils.log("info", "Executing AflPlayerLoader ...");

			List<AflTeam> aflTeams = aflTeamService.findAll();

			loggerUtils.log("info", "Processing teams: {}", aflTeams);

			processTeams(aflTeams);

			loggerUtils.log("info", "AflPlayerLoader Complete");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}

	}

	private void processTeams(List<AflTeam> aflTeams) throws Exception {

		List<AflPlayer> aflPlayers = new ArrayList<>();

		AflPlayerLoaderHtmlHandler playerHtmlLoader = new AflPlayerLoaderHtmlHandler();

		for(AflTeam team : aflTeams) {

			loggerUtils.log("info", "Working on team: {}", team.getTeamId());

			String teamListUrlS = team.getWebsite() + "/" + team.getSeniorUri();
			loggerUtils.log("info", "Senior list URL: {}", teamListUrlS);

			aflPlayers.addAll(playerHtmlLoader.execute(team.getTeamId(), teamListUrlS));

			loggerUtils.log("info", "Seniors added to list");

			if(team.getRookieUri() != null && !team.getRookieUri().equals("")) {
				String teamListUrlR = team.getWebsite() + "/" + team.getRookieUri();
				loggerUtils.log("info", "Rookie list URL: {}", teamListUrlS);

				aflPlayers.addAll(playerHtmlLoader.execute(team.getTeamId(), teamListUrlR));

				loggerUtils.log("info", "Rookies added to list");
			} else {
				loggerUtils.log("info", "No rookie list");
			}
		}

		aflPlayers = removeDuplicates(aflPlayers);

		loggerUtils.log("info", "Saving players to database ...");
		aflPlayerService.replaceAll(aflPlayers);

		loggerUtils.log("info", "Creating afl-dfl player cross references");
		crossRefAflDflPlayers(aflPlayers);
	}

	private List<AflPlayer> removeDuplicates(List<AflPlayer> aflPlayers) {

		List<AflPlayer> checkedPlayers = new ArrayList<>();

		for(AflPlayer player : aflPlayers) {
			boolean playerExists = false;
			for(AflPlayer checkedPlayer : checkedPlayers) {
				if(player.getPlayerId().contentEquals(checkedPlayer.getPlayerId())) {
					playerExists = true;
					break;
				}
			}
			if(playerExists) {
				loggerUtils.log("info", "Player EXISTS!!; Player: {}", player);
			} else {
				checkedPlayers.add(player);
			}
		}

		return checkedPlayers;
	}

	private void crossRefAflDflPlayers(List<AflPlayer> aflPlayers) {

		Map<String, DflPlayer> dflPlayerCrossRefs = dflPlayerService.getCrossRefPlayers();
		List<AflPlayer> aflUnmatchedPlayers = new ArrayList<>();

		Map<String, DflPlayer> dflPlayerUpdates = new HashMap<>();
		Map<Integer, AflPlayer> aflPlayerUpdates = new HashMap<>();

		for(AflPlayer aflPlayer : aflPlayers) {
			String aflPlayerCrossRef = (aflPlayer.getName().replaceAll("[^a-zA-Z]", "") + "-" + globalsService.getAflTeamMap(aflPlayer.getTeamId().toLowerCase())).toLowerCase();
			loggerUtils.log("info", "Searching for player: {}", aflPlayerCrossRef);
			DflPlayer dflPlayer = dflPlayerCrossRefs.get(aflPlayerCrossRef);

			if(dflPlayer != null) {

				int dflPlayerId = dflPlayer.getPlayerId();
				String aflPlayerId = aflPlayer.getPlayerId();

				loggerUtils.log("info", "Matched player - CrossRef: {}, DflPlayerId: {}, AflPlayerId {}", aflPlayerCrossRef, dflPlayerId, aflPlayerId);

				dflPlayerUpdates.put(aflPlayerId, dflPlayer);
				aflPlayerUpdates.put(dflPlayerId, aflPlayer);

				//dflPlayer.setAflPlayerId(aflPlayerId);
				//aflPlayer.setDflPlayerId(dflPlayerId);

				//dflPlayerService.update(dflPlayer);
				//aflPlayerService.update(aflPlayer);

				dflPlayerCrossRefs.remove(aflPlayerCrossRef);
			} else {
				loggerUtils.log("info", "Unmatched AFL player: {}", aflPlayer);
				aflUnmatchedPlayers.add(aflPlayer);
			}
		}

		List<DflUnmatchedPlayer> unmatchedPlayers = new ArrayList<>();

		for (Map.Entry<String, DflPlayer> entry : dflPlayerCrossRefs.entrySet()) {
		    String crossRef = entry.getKey();
		    DflPlayer dflPlayer = entry.getValue();

		    boolean matched = false;

		    String dflCheckOne = ((dflPlayer.getFirstName() + dflPlayer.getInitial() + dflPlayer.getLastName()).replaceAll("[^a-zA-Z]", "") + "-" + dflPlayer.getAflClub()).toLowerCase();
		    String dflCheckTwo = (dflPlayer.getLastName().replaceAll("[^a-zA-Z]", "") + "-" + dflPlayer.getAflClub()).toLowerCase();
		    String dflCheckThree = (dflPlayer.getFirstName().replaceAll("[^a-zA-Z]", "") + "-" + dflPlayer.getAflClub()).toLowerCase();

		    for(AflPlayer aflPlayer : aflUnmatchedPlayers) {

		    	String aflTeamName = globalsService.getAflTeamMap(aflPlayer.getTeamId().toLowerCase());

		    	String aflCheckOne = (aflPlayer.getName().replaceAll("[^a-zA-Z]", "") + "-" + aflTeamName).toLowerCase();

		    	loggerUtils.log("info", "Check one {} vs {}", dflCheckOne, aflCheckOne);

		    	if(dflCheckOne.equals(aflCheckOne)) {
		    		int dflPlayerId = dflPlayer.getPlayerId();
					String aflPlayerId = aflPlayer.getPlayerId();

					loggerUtils.log("info", "Matched player on Check One - CrossRef: {}, DflPlayerId: {}, AflPlayerId {}", crossRef, dflPlayerId, aflPlayerId);

					dflPlayerUpdates.put(aflPlayerId, dflPlayer);
					aflPlayerUpdates.put(dflPlayerId, aflPlayer);

					//player.setAflPlayerId(aflPlayerId);
					//aflPlayer.setDflPlayerId(dflPlayerId);

					//dflPlayerService.update(player);
					//aflPlayerService.update(aflPlayer);

		    		//dflPlayerCrossRefs.remove(crossRef);

		    		matched = true;
		    		break;
		    	}

		    	if(!matched) {
		    		String aflCheckTwo = (aflPlayer.getSecondName().replaceAll("[^a-zA-Z]", "") + "-" + aflTeamName).toLowerCase();

		    		loggerUtils.log("info", "Check two {} vs {}", dflCheckTwo, aflCheckTwo);

		    		if(dflCheckTwo.equals(aflCheckTwo)) {
			    		int dflPlayerId = dflPlayer.getPlayerId();
						String aflPlayerId = aflPlayer.getPlayerId();

						loggerUtils.log("info", "Matched player on Check Two - CrossRef: {}, DflPlayerId: {}, AflPlayerId {}", crossRef, dflPlayerId, aflPlayerId);

						dflPlayerUpdates.put(aflPlayerId, dflPlayer);
						aflPlayerUpdates.put(dflPlayerId, aflPlayer);

						//player.setAflPlayerId(aflPlayerId);
						//aflPlayer.setDflPlayerId(dflPlayerId);

						//dflPlayerService.update(player);
						//aflPlayerService.update(aflPlayer);

			    		//dflPlayerCrossRefs.remove(crossRef);

			    		matched = true;
			    		break;
		    		}
		    	}

		    	if(!matched) {
		    		String aflCheckThree = (aflPlayer.getFirstName().replaceAll("[^a-zA-Z]", "") + "-" + aflTeamName).toLowerCase();

		    		loggerUtils.log("info", "Check three {} vs {}", dflCheckThree, aflCheckThree);

		    		if(dflCheckThree.equals(aflCheckThree)) {
			    		int dflPlayerId = dflPlayer.getPlayerId();
						String aflPlayerId = aflPlayer.getPlayerId();

						loggerUtils.log("info", "Matched player on Check Two - CrossRef: {}, DflPlayerId: {}, AflPlayerId {}", crossRef, dflPlayerId, aflPlayerId);

						dflPlayerUpdates.put(aflPlayerId, dflPlayer);
						aflPlayerUpdates.put(dflPlayerId, aflPlayer);

						//player.setAflPlayerId(aflPlayerId);
						//aflPlayer.setDflPlayerId(dflPlayerId);

						//dflPlayerService.update(player);
						//aflPlayerService.update(aflPlayer);

			    		//dflPlayerCrossRefs.remove(crossRef);

			    		matched = true;
			    		break;
		    		}
		    	}

		    }

		    if(!matched) {
			    loggerUtils.log("info", "Unmatched player: {}", crossRef);

			    DflUnmatchedPlayer unmatchedPlayer = new DflUnmatchedPlayer();
			    unmatchedPlayer.setPlayerId(dflPlayer.getPlayerId());
			    unmatchedPlayer.setFirstName(dflPlayer.getFirstName());
			    unmatchedPlayer.setLastName(dflPlayer.getLastName());
			    unmatchedPlayer.setInitial(dflPlayer.getInitial());
			    unmatchedPlayer.setStatus(dflPlayer.getStatus());
			    unmatchedPlayer.setAflClub(dflPlayer.getAflClub());
			    unmatchedPlayer.setPosition(dflPlayer.getPosition());
			    unmatchedPlayer.setFirstYear(dflPlayer.isFirstYear());

			    unmatchedPlayers.add(unmatchedPlayer);
		    }
		}

		dflPlayerService.bulkUpdateAflPlayerId(dflPlayerUpdates);
		aflPlayerService.bulkUpdateDflPlayerId(aflPlayerUpdates);

		dflUnmatchedPlayerService.replaceAll(unmatchedPlayers);

	}

	public static void main(String[] args) {

		AflPlayerLoaderHandler aflPlayerLoader = new AflPlayerLoaderHandler();

		java.security.Security.setProperty("networkaddress.cache.ttl", "30");
		java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");

		aflPlayerLoader.execute();

	}


}