package net.dflmngr.model.dao.impl;

import java.util.List;

import jakarta.persistence.criteria.Predicate;

import net.dflmngr.model.dao.DflPlayerDao;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflPlayer_;

public class DflPlayerDaoImpl extends GenericDaoImpl<DflPlayer, Integer> implements DflPlayerDao {
	
	public DflPlayerDaoImpl() {
		super(DflPlayer.class);
	}
	
	public DflPlayer findByAflPlayerId(String aflPlayerId) {
		DflPlayer player = null;
		
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate equals = criteriaBuilder.equal(entity.get(DflPlayer_.aflPlayerId), aflPlayerId);
		
		criteriaQuery.where(criteriaBuilder.and(equals));
		List<DflPlayer> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		if(!entitys.isEmpty()) {
			player = entitys.get(0);
		}
		
		return player;
	}
	
	public List<DflPlayer> findAdamGoodesEligible() {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate equals = criteriaBuilder.equal(entity.get(DflPlayer_.isFirstYear), true);
		
		criteriaQuery.where(criteriaBuilder.and(equals));
		List<DflPlayer> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
	
	public List<DflPlayer> findByTeam(String team) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate equals = criteriaBuilder.equal(entity.get(DflPlayer_.aflClub), team);
		
		criteriaQuery.where(criteriaBuilder.and(equals));
		List<DflPlayer> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
}
