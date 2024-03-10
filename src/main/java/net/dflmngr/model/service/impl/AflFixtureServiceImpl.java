package net.dflmngr.model.service.impl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dflmngr.model.dao.AflFixtureDao;
import net.dflmngr.model.dao.impl.AflFixtureDaoImpl;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.entity.keys.AflFixturePK;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.utils.DflmngrUtils;

public class AflFixtureServiceImpl extends GenericServiceImpl<AflFixture, AflFixturePK> implements AflFixtureService {

	private AflFixtureDao dao;
	
	public AflFixtureServiceImpl() {
		dao = new AflFixtureDaoImpl();
		setDao(dao);
	}
	
	public List<AflFixture> getAflFixturesForRound(int round) {
		return dao.findAflFixturesForRound(round);
	}
	
	public Map<Integer, List<AflFixture>> getAflFixturneRoundBlocks() {
	
		Map<Integer, List<AflFixture>> fxitureRoundBlocks = new HashMap<>();
		
		List<AflFixture> fullFixture = dao.findAll();
		
		for(AflFixture fixture : fullFixture) {
			int roundKey = fixture.getRound();
			List<AflFixture> fixtureBlock = null;
			
			if(fxitureRoundBlocks.containsKey(roundKey)) {
				fixtureBlock = fxitureRoundBlocks.get(roundKey);
			} else {
				fixtureBlock = new ArrayList<>();
			}
			
			fixtureBlock.add(fixture);
			fxitureRoundBlocks.put(roundKey, fixtureBlock);
		}
		
		return fxitureRoundBlocks;
	}

	public AflFixture getAflFixtureForRoundAndTeam(int round, String team) {
		return dao.findAflFixtureForRoundAndTeam(round, team);
	}
	
	public List<AflFixture> getAflFixturesPlayedForRound(int round) {
		
		List<AflFixture> playedFixtures = new ArrayList<>();
		List<AflFixture> aflFixtures = dao.findAflFixturesForRound(round);
		
		for(AflFixture fixture : aflFixtures) {
			if(fixture.getEndTime() != null) {
				playedFixtures.add(fixture);
			}
			
		}
		
		return playedFixtures;
	}
	
	public AflFixture getPlayedGame(int round, int game) {
		
		AflFixture playedFixture = null;
		
		AflFixturePK aflFixturePK = new AflFixturePK();
		aflFixturePK.setRound(round);
		aflFixturePK.setGame(game);
		
		AflFixture fixture = get(aflFixturePK);

		if(fixture.getEndTime() != null) {
			playedFixture = fixture;
		}
		
		return playedFixture;
	}
	
	public List<String> getAflTeamsPlayedForRound(int round) {
		List<String> playedTeams = new ArrayList<>();
		
		List<AflFixture> playedFixtures = getAflFixturesPlayedForRound(round);
		
		for(AflFixture fixture : playedFixtures) {
			playedTeams.add(fixture.getHomeTeam());
			playedTeams.add(fixture.getAwayTeam());
		}
		
		return playedTeams;
	}
	
	public List<AflFixture> getIncompleteFixtures() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
		return dao.findIncompleteAflFixtures(now);
	}
	
	public List<AflFixture> getFixturesToScrape() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
		return dao.findFixturesToScrape(now);
	}
	
	public List<Integer> getAflRoundsToScrape() {
		List<AflFixture> fixtures = getFixturesToScrape();
		
		List<Integer> rounds = new ArrayList<>();
		for(AflFixture fixture : fixtures) {
			if(!rounds.contains(fixture.getRound())) {
				rounds.add(fixture.getRound());
			}
		}
		
		return rounds;
	}
	
	public boolean getAflRoundComplete(int round) {
		boolean complete = false;
		
		List<AflFixture> incomleteFixtures = dao.findIncompleteFixturesForRound(round);
		
		if(incomleteFixtures == null || incomleteFixtures.isEmpty()) {
			complete = true;
		}
		
		return complete;
	}

	public void updateLoadedFixtures(List<AflFixture> updatedFixtures) {
		List<AflFixture> saveFixtures = new ArrayList<>();

		for(AflFixture updatedFixture : updatedFixtures) {
			AflFixturePK aflFixturePK = new AflFixturePK();
			aflFixturePK.setRound(updatedFixture.getRound());
			aflFixturePK.setGame(updatedFixture.getGame());

			AflFixture fixture = get(aflFixturePK);
			if(fixture == null) {
				fixture = new AflFixture();

				fixture.setRound(updatedFixture.getRound());
				fixture.setGame(updatedFixture.getGame());
			}

			fixture.setHomeTeam(updatedFixture.getHomeTeam());
			fixture.setAwayTeam(updatedFixture.getAwayTeam());
			fixture.setGround(updatedFixture.getGround());
			fixture.setStartTime(updatedFixture.getStartTime());
			fixture.setTimezone(updatedFixture.getTimezone());

			saveFixtures.add(fixture);
		}

		updateAll(saveFixtures, false);
	}

	public int getMaxAflRound() {
		return dao.findMaxAflRound();
	}

	public int getRefreshFixtureStart() {
		return dao.findRefreshFixtureStart();
	}
}
