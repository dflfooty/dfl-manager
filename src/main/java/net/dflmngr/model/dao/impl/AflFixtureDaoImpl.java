package net.dflmngr.model.dao.impl;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.criteria.CriteriaBuilder.In;

import net.dflmngr.model.dao.AflFixtureDao;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.AflFixture_;
import net.dflmngr.model.entity.keys.AflFixturePK;

public class AflFixtureDaoImpl extends GenericDaoImpl<AflFixture, AflFixturePK> implements AflFixtureDao {
	
	public AflFixtureDaoImpl() {
		super(AflFixture.class);
	}
	
	public List<AflFixture> findAflFixturesForRound(int round) {
		
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(AflFixture_.round), round);
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals));
		return  entityManager.createQuery(criteriaQuery).getResultList();
	}
	
	public List<AflFixture> findIncompleteAflFixtures(ZonedDateTime time) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate startLess = criteriaBuilder.lessThan(entity.get(AflFixture_.startTime), time);
		Predicate endNull = criteriaBuilder.isNull(entity.get(AflFixture_.endTime));
		
		criteriaQuery.where(criteriaBuilder.and(startLess, endNull));
		return entityManager.createQuery(criteriaQuery).getResultList();
	}
	
	public List<AflFixture> findFixturesToScrape(ZonedDateTime time) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate startLess = criteriaBuilder.lessThan(entity.get(AflFixture_.startTime), time);
		Predicate statsDownloadedNull = criteriaBuilder.isNull(entity.get(AflFixture_.statsDownloaded));
		Predicate statsDownloadedFalse = criteriaBuilder.isFalse(entity.get(AflFixture_.statsDownloaded));
		Predicate statsDownloadedNullOrFalse = criteriaBuilder.or(statsDownloadedNull, statsDownloadedFalse);
		
		criteriaQuery.where(criteriaBuilder.and(startLess, statsDownloadedNullOrFalse));
		return entityManager.createQuery(criteriaQuery).getResultList();
	}
	
	public List<AflFixture> findIncompleteFixturesForRound(int round) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate roundEquals = criteriaBuilder.equal(entity.get(AflFixture_.round), round);
		Predicate endTimeNull = criteriaBuilder.isNull(entity.get(AflFixture_.endTime));
		
		criteriaQuery.where(criteriaBuilder.and(roundEquals, endTimeNull));
		return entityManager.createQuery(criteriaQuery).getResultList();
	}

	public int findMaxAflRound() {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
		entity = criteriaQuery.from(entityClass);

		Expression<Integer> maxRound = criteriaBuilder.max(entity.get(AflFixture_.round));
		Predicate startTimeNotNull = criteriaBuilder.isNotNull(entity.get(AflFixture_.startTime));

		criteriaQuery.select(maxRound).where(startTimeNotNull);
		return entityManager.createQuery(criteriaQuery).getSingleResult();
	}

	public int findRefreshFixtureStart() {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
		entity = criteriaQuery.from(entityClass);

		Subquery<Integer> criteriaSubquery = criteriaQuery.subquery(Integer.class);
		Root<AflFixture> subqueryEntity = criteriaSubquery.from(entityClass);

		Predicate statsDownloadedFalse = criteriaBuilder.isFalse(subqueryEntity.get(AflFixture_.statsDownloaded));
		Expression<Long> count = criteriaBuilder.count(subqueryEntity.get(AflFixture_.round));
		Predicate countEquals9 = criteriaBuilder.equal(count, 9);

		criteriaSubquery.select(subqueryEntity.get(AflFixture_.round))
			.where(statsDownloadedFalse)
			.groupBy(subqueryEntity.get(AflFixture_.round))
			.having(countEquals9);

		Expression<Integer> min = criteriaBuilder.min(entity.get(AflFixture_.round));
		In<Integer> roundIn = criteriaBuilder.in(entity.get(AflFixture_.round));
		roundIn.value(criteriaSubquery);

		criteriaQuery.select(min).where(roundIn);
		return entityManager.createQuery(criteriaQuery).getSingleResult();
	}
	
}
