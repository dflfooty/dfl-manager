package net.dflmngr.model.dao.impl;

import jakarta.persistence.criteria.Predicate;
import net.dflmngr.model.dao.AflTeamDao;
import net.dflmngr.model.entity.AflTeam;
import net.dflmngr.model.entity.AflTeam_;

public class AflTeamDaoImpl extends GenericDaoImpl<AflTeam, String> implements AflTeamDao {
	public AflTeamDaoImpl() {
		super(AflTeam.class);
	}

	public AflTeam findAflTeamByName(String name) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate equals = criteriaBuilder.equal(entity.get(AflTeam_.name), name);
		
		criteriaQuery.where(criteriaBuilder.and(equals));
		return entityManager.createQuery(criteriaQuery).getSingleResult();
	}
}
