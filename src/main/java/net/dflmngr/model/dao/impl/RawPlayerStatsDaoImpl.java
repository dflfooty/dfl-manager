package net.dflmngr.model.dao.impl;

import java.util.List;

import javax.persistence.criteria.Predicate;

import net.dflmngr.model.dao.RawPlayerStatsDao;
import net.dflmngr.model.entity.RawPlayerStats;
import net.dflmngr.model.entity.RawPlayerStats_;
import net.dflmngr.model.entity.keys.RawPlayerStatsPK;

public class RawPlayerStatsDaoImpl extends GenericDaoImpl<RawPlayerStats, RawPlayerStatsPK> implements RawPlayerStatsDao {
	
	public RawPlayerStatsDaoImpl() {
		super(RawPlayerStats.class);
	}
	
	public List<RawPlayerStats> findForRound(int round) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(RawPlayerStats_.round), round);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals));
		List<RawPlayerStats> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
	
	public void deleteStatsForRoundAndTeam(int round, String team) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaDelete = criteriaBuilder.createCriteriaDelete(entityClass);
		entity = criteriaDelete.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(RawPlayerStats_.round), round);
		Predicate teamEquals = criteriaBuilder.equal(entity.get(RawPlayerStats_.team), team);
		criteriaDelete.where(criteriaBuilder.and(roundEquals, teamEquals));
		
		entityManager.createQuery(criteriaDelete).executeUpdate();
		
	}
	
	public List<RawPlayerStats> findForRoundAndTeam(int round, String team) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(RawPlayerStats_.round), round);
		Predicate teamEquals = criteriaBuilder.equal(entity.get(RawPlayerStats_.team), team);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals, teamEquals));
		List<RawPlayerStats> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
}
