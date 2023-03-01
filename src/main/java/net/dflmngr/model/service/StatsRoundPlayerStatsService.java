package net.dflmngr.model.service;

import java.util.List;
import java.util.Map;

import net.dflmngr.model.entity.StatsRoundPlayerStats;
import net.dflmngr.model.entity.keys.StatsRoundPlayerStatsPK;

public interface StatsRoundPlayerStatsService extends GenericService<StatsRoundPlayerStats, StatsRoundPlayerStatsPK> {
	
	public List<StatsRoundPlayerStats> getForRound(int round);
	public void replaceAllForRound(int round, List<StatsRoundPlayerStats> playerStats);
	public Map<String, StatsRoundPlayerStats> getForRoundWithKey(int round);
	public void removeStatsForRoundAndTeam(int round, String team);
	public List<StatsRoundPlayerStats> getForRoundAndTeam(int round, String team);
}
