package net.dflmngr.model.dao.impl;

import java.util.List;

import javax.persistence.criteria.Predicate;

import net.dflmngr.model.dao.DflSelectionIdsDao;
import net.dflmngr.model.entity.DflSelectionIds;
import net.dflmngr.model.entity.DflSelectionIds_;

public class DflSelectionIdsDaoImpl extends GenericDaoImpl<DflSelectionIds, Integer> implements DflSelectionIdsDao {

	public DflSelectionIdsDaoImpl() {
		super(DflSelectionIds.class);
	}
	
	public List<DflSelectionIds> findSelectionIdForTeam(int round, String teamCode, String id) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(DflSelectionIds_.round), round);
		Predicate teamCodeEquals = criteriaBuilder.equal(entity.get(DflSelectionIds_.teamCode), teamCode);
		Predicate selectionIdEquals = criteriaBuilder.equal(entity.get(DflSelectionIds_.selectionId), id);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals, teamCodeEquals, selectionIdEquals));
		List<DflSelectionIds> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
}
