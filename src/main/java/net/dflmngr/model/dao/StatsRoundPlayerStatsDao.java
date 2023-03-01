package net.dflmngr.model.dao;

import java.util.List;

import net.dflmngr.model.entity.StatsRoundPlayerStats;
import net.dflmngr.model.entity.keys.StatsRoundPlayerStatsPK;

public interface StatsRoundPlayerStatsDao extends GenericDao<StatsRoundPlayerStats, StatsRoundPlayerStatsPK> {
	
	public List<StatsRoundPlayerStats> findForRound(int round);
	public void deleteStatsForRoundAndTeam(int round, String team);
	public List<StatsRoundPlayerStats> findForRoundAndTeam(int round, String team);
	
}
