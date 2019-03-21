package net.dflmngr.model.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dflmngr.model.dao.DflPlayerDao;
import net.dflmngr.model.dao.impl.DflPlayerDaoImpl;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.service.DflPlayerService;

public class DflPlayerServiceImpl extends GenericServiceImpl<DflPlayer, Integer> implements DflPlayerService {
	
	private DflPlayerDao dao;
	
	public DflPlayerServiceImpl() {
		dao = new DflPlayerDaoImpl();
		super.setDao(dao);
	}
	
	public DflPlayer getByAflPlayerId(String aflPlayerId) {
		DflPlayer dflPlayer = null;
		dflPlayer = dao.findByAflPlayerId(aflPlayerId);
		return dflPlayer;
	}
	
	public List<DflPlayer> getAdamGoodesEligible() {
		List<DflPlayer> adamGoodesEligible = null;
		adamGoodesEligible = dao.findAdamGoodesEligible();
		return adamGoodesEligible;
	}
	
	public Map<String, DflPlayer> getCrossRefPlayers() {
		Map<String, DflPlayer> crossRefPlayers = new HashMap<>();
		List<DflPlayer> allPlayers = dao.findAll();
		
		for(DflPlayer player : allPlayers) {
			String crossRefId = ((player.getFirstName() +  player.getLastName()).replaceAll("[^a-zA-Z]", "") + "-" + player.getAflClub()).toLowerCase();
			crossRefPlayers.put(crossRefId, player);
		}
		
		return crossRefPlayers;
	}
	
	public void bulkUpdateAflPlayerId(Map<String, DflPlayer> entitys) {
		dao.beginTransaction();
		
		for(Map.Entry<String, DflPlayer> entry : entitys.entrySet()) {
		    String aflPlayerId = entry.getKey();
		    DflPlayer player = entry.getValue();
		    
		    player.setAflPlayerId(aflPlayerId);
		}
		
		dao.commit();
	}
	
	public List<DflPlayer> getByTeam(String team) {
		List<DflPlayer> playersByTeam = null;
		playersByTeam = dao.findByTeam(team);
		return playersByTeam;
	}
}
