package net.dflmngr.model.service.impl;

import java.util.List;

import net.dflmngr.model.dao.InsAndOutsDao;
import net.dflmngr.model.dao.impl.InsAndOutsDaoImpl;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.service.InsAndOutsService;

public class InsAndOutsServiceImpl extends GenericServiceImpl<InsAndOuts, Integer> implements InsAndOutsService {
	
	InsAndOutsDao insAndOutsDao;
	
	public InsAndOutsServiceImpl() {
		insAndOutsDao = new InsAndOutsDaoImpl();
		super.setDao(insAndOutsDao);
	}
	
	public void saveTeamInsAndOuts(List<InsAndOuts> insAndOuts) {
		int round = insAndOuts.get(0).getRound();
		String teamCode = insAndOuts.get(0).getTeamCode();
		
		List<InsAndOuts> existingInsAndOuts = insAndOutsDao.findByTeamAndRound(round, teamCode);
		
		insAndOutsDao.beginTransaction();
		
		if(!existingInsAndOuts.isEmpty()) {
			for(InsAndOuts delete : existingInsAndOuts) {
				insAndOutsDao.remove(delete);
			}
		}
		
		insAndOutsDao.flush();
		
		for(InsAndOuts insert : insAndOuts) {
			insAndOutsDao.persist(insert);
		}
		
		insAndOutsDao.commit();
	}
	
	public List<InsAndOuts> getByTeamAndRound(int round, String teamCode) {
		return insAndOutsDao.findByTeamAndRound(round, teamCode);
	}
	
	public void removeForRound(int round) {
		List<InsAndOuts> existingInsAndOuts = insAndOutsDao.findByRound(round);
		
		insAndOutsDao.beginTransaction();
		
		if(!existingInsAndOuts.isEmpty()) {
			for(InsAndOuts delete : existingInsAndOuts) {
				insAndOutsDao.remove(delete);
			}
		}
		
		insAndOutsDao.commit();
	}
}
