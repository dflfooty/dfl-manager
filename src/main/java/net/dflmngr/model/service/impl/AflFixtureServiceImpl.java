package net.dflmngr.model.service.impl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.TimeZone;
import java.util.stream.Collectors;
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
		List<AflFixture> aflFixtures = dao.findAflFixturesForRound(round);
		return aflFixtures;
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
	
	public List<AflFixture> getAflFixturesPlayedForRound(int round) throws Exception {
		
		List<AflFixture> playedFixtures = new ArrayList<>();
		List<AflFixture> aflFixtures = dao.findAflFixturesForRound(round);
		
		for(AflFixture fixture : aflFixtures) {
			if(fixture.getEndTime() != null) {
				playedFixtures.add(fixture);
			}
			
		}
		
		return playedFixtures;
	}
	
	public AflFixture getPlayedGame(int round, int game) throws Exception {
		
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
	
	public List<String> getAflTeamsPlayedForRound(int round) throws Exception {
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
		
		List<AflFixture> incompleteFixtures = dao.findIncompleteAflFixtures(now);
		
		return incompleteFixtures;
	}
	
	public List<AflFixture> getFixturesToScrape() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
		
		List<AflFixture> fixturesToScrape = dao.findFixturesToScrape(now);
		
		return fixturesToScrape;
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
		Map<String, AflFixture> updatedFixturesData = updatedFixtures.stream()
		.collect(Collectors.toMap(item -> (item.getRound() + "-" + item.getGame()), item -> item));

		List<AflFixture> currentFixtures = dao.findAll();
		List<AflFixture> saveFixtures = new ArrayList<>();

		for(AflFixture fixture : currentFixtures) {
			String key = fixture.getRound() + "-" + fixture.getGame();
			AflFixture updatedFixture = updatedFixturesData.get(key);

			fixture.setHomeTeam(updatedFixture.getHomeTeam());
			fixture.setAwayTeam(updatedFixture.getAwayTeam());
			fixture.setStartTime(updatedFixture.getStartTime());
			
			saveFixtures.add(fixture);
		}

		updateAll(saveFixtures, false);
	}

	public int getMaxAflRound() {
		int maxAflRound = dao.findMaxAflRound();
		return maxAflRound;
	}
}
