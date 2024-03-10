package net.dflmngr.model.dao.impl;

import java.util.List;

import jakarta.persistence.criteria.Predicate;

import net.dflmngr.model.dao.InsAndOutsDao;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.entity.InsAndOuts_;

public class InsAndOutsDaoImpl extends GenericDaoImpl<InsAndOuts, Integer>implements InsAndOutsDao {
	
	public InsAndOutsDaoImpl() {
		super(InsAndOuts.class);
	}
	
	public List<InsAndOuts> findByTeamAndRound(int round, String teamCode) {		
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(InsAndOuts_.round), round);
		Predicate teamCodeEquals = criteriaBuilder.equal(entity.get(InsAndOuts_.teamCode), teamCode);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals, teamCodeEquals));
		List<InsAndOuts> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
	
	public List<InsAndOuts> findByRound(int round) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(InsAndOuts_.round), round);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals));
		List<InsAndOuts> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
}
