package net.dflmngr.model.dao;

import java.util.List;

import net.dflmngr.model.entity.DflRoundInfo;

public interface DflRoundInfoDao extends GenericDao<DflRoundInfo, Integer> {
	public List<DflRoundInfo> findRoundsByAflRounds(List<Integer> aflRounds);
}
