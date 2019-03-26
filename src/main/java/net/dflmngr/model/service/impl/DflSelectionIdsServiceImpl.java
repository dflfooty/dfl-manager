package net.dflmngr.model.service.impl;

import java.util.List;

import net.dflmngr.model.dao.DflSelectionIdsDao;
import net.dflmngr.model.dao.impl.DflSelectionIdsDaoImpl;
import net.dflmngr.model.entity.DflSelectionIds;
import net.dflmngr.model.service.DflSelectionIdsService;

public class DflSelectionIdsServiceImpl extends GenericServiceImpl<DflSelectionIds, Integer> implements DflSelectionIdsService {

	DflSelectionIdsDao dflSelectionIdsDao;
	
	public DflSelectionIdsServiceImpl() {
		dflSelectionIdsDao = new DflSelectionIdsDaoImpl();
		super.setDao(dflSelectionIdsDao);
	}
	
	public boolean selectionIdExists(int round, String teamCode, String id) {
	
		boolean selectionIdExists = false;
		
		List<DflSelectionIds> selectionIds = dflSelectionIdsDao.findSelectionIdForTeam(round, teamCode, id);
		
		if(selectionIds != null && selectionIds.size() > 0) {
			selectionIdExists = true;
		}
		
		return selectionIdExists;
	}	
}
