package net.dflmngr.model.dao.impl;

import java.util.List;

import jakarta.persistence.criteria.Predicate;

import net.dflmngr.model.dao.StatsRoundPlayerStatsDao;
import net.dflmngr.model.entity.StatsRoundPlayerStats;
import net.dflmngr.model.entity.StatsRoundPlayerStats_;
import net.dflmngr.model.entity.keys.StatsRoundPlayerStatsPK;

public class StatsRoundPlayerStatsDaoImpl extends GenericDaoImpl<StatsRoundPlayerStats, StatsRoundPlayerStatsPK> implements StatsRoundPlayerStatsDao {
	
	public StatsRoundPlayerStatsDaoImpl() {
		super(StatsRoundPlayerStats.class);
	}
	
	public List<StatsRoundPlayerStats> findForRound(int round) {
		
		entityManager.clear();
		
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(StatsRoundPlayerStats_.round), round);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals));
		return entityManager.createQuery(criteriaQuery).getResultList();
	}
	
	public void deleteStatsForRoundAndTeam(int round, String team) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaDelete = criteriaBuilder.createCriteriaDelete(entityClass);
		entity = criteriaDelete.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(StatsRoundPlayerStats_.round), round);
		Predicate teamEquals = criteriaBuilder.equal(entity.get(StatsRoundPlayerStats_.team), team);
		criteriaDelete.where(criteriaBuilder.and(roundEquals, teamEquals));
		
		entityManager.createQuery(criteriaDelete).executeUpdate();
	}
	
	public List<StatsRoundPlayerStats> findForRoundAndTeam(int round, String team) {
		
		entityManager.clear();
		
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(StatsRoundPlayerStats_.round), round);
		Predicate teamEquals = criteriaBuilder.equal(entity.get(StatsRoundPlayerStats_.team), team);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals, teamEquals));
		return entityManager.createQuery(criteriaQuery).getResultList();
	}
}
