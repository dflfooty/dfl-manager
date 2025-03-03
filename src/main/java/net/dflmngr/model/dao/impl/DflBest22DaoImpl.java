package net.dflmngr.model.dao.impl;

import java.util.List;

import jakarta.persistence.criteria.Predicate;

import net.dflmngr.model.dao.DflBest22Dao;
import net.dflmngr.model.entity.DflBest22;
import net.dflmngr.model.entity.DflBest22_;

public class DflBest22DaoImpl extends GenericDaoImpl<DflBest22, Integer> implements DflBest22Dao {
	public DflBest22DaoImpl() {
		super(DflBest22.class);
	}
	
	public List<DflBest22> findForRound(int round) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate equals = criteriaBuilder.equal(entity.get(DflBest22_.round), round);
		
		criteriaQuery.where(equals);
		List<DflBest22> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
}
