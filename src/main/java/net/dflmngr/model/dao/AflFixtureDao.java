package net.dflmngr.model.dao;

import java.time.ZonedDateTime;
import java.util.List;

import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.keys.AflFixturePK;

public interface AflFixtureDao extends GenericDao<AflFixture, AflFixturePK> {
	
	public List<AflFixture> findAflFixturesForRound(int round);
	public List<AflFixture> findIncompleteAflFixtures(ZonedDateTime now);
}
