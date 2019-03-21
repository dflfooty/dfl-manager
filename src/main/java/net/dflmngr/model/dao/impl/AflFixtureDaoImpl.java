package net.dflmngr.model.dao.impl;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.criteria.Predicate;

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
		List<AflFixture> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
	
	public List<AflFixture> findIncompleteAflFixtures(ZonedDateTime time) {
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		entity = criteriaQuery.from(entityClass);
		
		Predicate startLess = criteriaBuilder.lessThan(entity.get(AflFixture_.startTime), time);
		Predicate endNull = criteriaBuilder.isNull(entity.get(AflFixture_.endTime));
		
		criteriaQuery.where(criteriaBuilder.and(startLess, endNull));
		List<AflFixture> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
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
		List<AflFixture> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		
		return entitys;
	}
	
}
