package net.dflmngr.model.service;

import net.dflmngr.model.entity.DflSelectionIds;

public interface DflSelectionIdsService extends GenericService<DflSelectionIds, Integer> {
	public boolean selectionIdExists(int round, String teamCode, String id);
}
