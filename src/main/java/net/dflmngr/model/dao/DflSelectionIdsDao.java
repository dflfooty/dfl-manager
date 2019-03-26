package net.dflmngr.model.dao;

import java.util.List;

import net.dflmngr.model.entity.DflSelectionIds;

public interface DflSelectionIdsDao extends GenericDao<DflSelectionIds, Integer> {
	public List<DflSelectionIds> findSelectionIdForTeam(int round, String teamCode, String id);
}
