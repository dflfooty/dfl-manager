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
		
		//Date now = new Date();
		//Calendar nowCal = Calendar.getInstance();
		//nowCal.setTime(now);
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
		
		for(AflFixture fixture : aflFixtures) {
			
			//Calendar startCal = Calendar.getInstance();
			//startCal.setTimeZone(TimeZone.getTimeZone(DflmngrUtils.defaultTimezone));
			//startCal.setTime(DflmngrUtils.dateDbFormat.parse(fixture.getStart()));
			//startCal.add(Calendar.HOUR_OF_DAY, 3);
			ZonedDateTime gameEndTime = fixture.getStartTime().plusHours(3);
						
			//if(nowCal.after(startCal)) {
			if(now.isAfter(gameEndTime)) {
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
		
		//Date now = new Date();
		//Calendar nowCal = Calendar.getInstance();
		//nowCal.setTimeZone(TimeZone.getTimeZone(DflmngrUtils.defaultTimezone));
		//nowCal.setTime(now);
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(DflmngrUtils.defaultTimezone));
		
		
		//Calendar startCal = Calendar.getInstance();
		//startCal.setTimeZone(TimeZone.getTimeZone(DflmngrUtils.defaultTimezone));
		//startCal.setTime(DflmngrUtils.dateDbFormat.parse(fixture.getStart()));
		//startCal.add(Calendar.HOUR_OF_DAY, 3);
		ZonedDateTime gameEndTime = fixture.getStartTime().plusHours(3);
		
		//if(nowCal.after(startCal)) {
		if(now.isAfter(gameEndTime)) {
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
		
		List<AflFixture> fixturesToScrape = dao.findIncompleteAflFixtures(now);
		
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
}
