package net.dflmngr.model.service;

import java.util.List;
import java.util.Map;

import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.keys.AflFixturePK;

public interface AflFixtureService extends GenericService<AflFixture, AflFixturePK> {
	
	public List<AflFixture> getAflFixturesForRound(int round);
	public Map<Integer, List<AflFixture>> getAflFixturneRoundBlocks();
	public List<AflFixture> getAflFixturesPlayedForRound(int round) throws Exception;
	public AflFixture getPlayedGame(int round, int game) throws Exception;
	public List<String> getAflTeamsPlayedForRound(int round) throws Exception;
	public List<AflFixture> getIncompleteFixtures();
	public List<AflFixture> getFixturesToScrape();
	public List<Integer> getAflRoundsToScrape();
}
