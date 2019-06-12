package net.dflmngr.model.service.impl;

import java.util.List;

import net.dflmngr.model.dao.DflRoundInfoDao;
import net.dflmngr.model.dao.impl.DflRoundInfoDaoImpl;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.service.DflRoundInfoService;

public class DflRoundInfoServiceImpl extends GenericServiceImpl<DflRoundInfo, Integer> implements DflRoundInfoService {
	
	DflRoundInfoDao dao;
	
	public DflRoundInfoServiceImpl() {
		dao = new DflRoundInfoDaoImpl();
		setDao(dao);
	}
	
	public List<DflRoundInfo> getRoundsByAflRounds(List<Integer> aflRounds) {
		List<DflRoundInfo> roundInfo = dao.findRoundsByAflRounds(aflRounds);
		return roundInfo;
	}
}
