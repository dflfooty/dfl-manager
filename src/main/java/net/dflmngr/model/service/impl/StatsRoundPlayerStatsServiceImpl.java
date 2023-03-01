package net.dflmngr.model.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dflmngr.model.dao.StatsRoundPlayerStatsDao;
import net.dflmngr.model.dao.impl.StatsRoundPlayerStatsDaoImpl;
import net.dflmngr.model.entity.StatsRoundPlayerStats;
import net.dflmngr.model.entity.keys.StatsRoundPlayerStatsPK;
import net.dflmngr.model.service.StatsRoundPlayerStatsService;

public class StatsRoundPlayerStatsServiceImpl extends GenericServiceImpl<StatsRoundPlayerStats, StatsRoundPlayerStatsPK> implements StatsRoundPlayerStatsService {

	StatsRoundPlayerStatsDao dao;
	
	public StatsRoundPlayerStatsServiceImpl() {
		dao = new StatsRoundPlayerStatsDaoImpl();
		setDao(dao);
	}
	
	public List<StatsRoundPlayerStats> getForRound(int round) {
		return dao.findForRound(round);
	}
	
	public Map<String, StatsRoundPlayerStats> getForRoundWithKey(int round) {
		Map<String, StatsRoundPlayerStats> playerStatsWithKey = new HashMap<>();
		
		List<StatsRoundPlayerStats> playerStats = dao.findForRound(round);
		
		for(StatsRoundPlayerStats stats : playerStats) {
			String key = stats.getTeam() + stats.getJumperNo();
			playerStatsWithKey.put(key, stats);
		}
		
		return playerStatsWithKey;
	}
	
	public void replaceAllForRound(int round, List<StatsRoundPlayerStats> playerStats) {
		
		dao.beginTransaction();
		
		List<StatsRoundPlayerStats> existingStats = getForRound(round);
		for(StatsRoundPlayerStats stats : existingStats) {
			delete(stats);
		}
		
		dao.flush();
		
		insertAll(playerStats, true);
		
		dao.commit();
	}
	
	public void removeStatsForRoundAndTeam(int round, String team) {
		
		dao.beginTransaction();
		
		List<StatsRoundPlayerStats> existingStats = getForRoundAndTeam(round, team);
		for(StatsRoundPlayerStats stats : existingStats) {
			delete(stats);
		}
		
		dao.commit();
	}
	
	public List<StatsRoundPlayerStats> getForRoundAndTeam(int round, String team) {
		return dao.findForRoundAndTeam(round, team);
	}
}
