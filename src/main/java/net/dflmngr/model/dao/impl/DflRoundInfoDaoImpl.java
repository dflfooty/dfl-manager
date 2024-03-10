package net.dflmngr.model.dao.impl;

import java.util.List;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.CriteriaBuilder.In;

import net.dflmngr.model.dao.DflRoundInfoDao;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundInfo_;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.entity.DflRoundMapping_;

public class DflRoundInfoDaoImpl extends GenericDaoImpl<DflRoundInfo, Integer> implements DflRoundInfoDao {
	public DflRoundInfoDaoImpl() {
		super(DflRoundInfo.class);
	}

	public List<DflRoundInfo> findRoundsByAflRounds(List<Integer> aflRounds) {

		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);

		Join<DflRoundInfo, DflRoundMapping> join = entity.join(DflRoundInfo_.roundMapping);

		In<Integer> roundMappingAflRoundIn = criteriaBuilder.in(join.get(DflRoundMapping_.aflRound));

		for(int aflRound: aflRounds) {
			roundMappingAflRoundIn.value(aflRound);
		}

		criteriaQuery.where(roundMappingAflRoundIn);

		return entityManager.createQuery(criteriaQuery.distinct(true)).getResultList();
	}
}
