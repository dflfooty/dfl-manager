package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

//import net.dflmngr.jndi.JndiProvider;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflAdamGoodes;
import net.dflmngr.model.entity.DflBest22;
import net.dflmngr.model.entity.DflCallumChambers;
import net.dflmngr.model.entity.DflFixture;
import net.dflmngr.model.entity.DflLadder;
import net.dflmngr.model.entity.DflMatthewAllen;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.entity.keys.DflFixturePK;
import net.dflmngr.model.entity.keys.DflTeamScoresPK;
import net.dflmngr.model.service.DflBest22Service;
import net.dflmngr.model.service.DflFixtureService;
import net.dflmngr.model.service.DflLadderService;
import net.dflmngr.model.service.DflMatthewAllenService;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.DflTeamScoresService;
import net.dflmngr.model.service.DflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.DflBest22ServiceImpl;
import net.dflmngr.model.service.impl.DflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflLadderServiceImpl;
import net.dflmngr.model.service.impl.DflMatthewAllenServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamScoresServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.utils.EmailUtils;

public class EndRoundHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "EndRound";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	DflMatthewAllenService 	dflMatthewAllenService;
	GlobalsService globalsService;
	DflPlayerService dflPlayerService;
	DflTeamPlayerService dflTeamPlayerService;
	DflTeamService dflTeamService;
	DflBest22Service dflBest22Service;
	DflLadderService dflLadderService;
	DflFixtureService dflFixtureService;
	DflTeamScoresService dflTeamScoresService;
	
	String emailOverride;

	public EndRoundHandler() {
		dflMatthewAllenService = new DflMatthewAllenServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflPlayerService = new DflPlayerServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		dflTeamService = new DflTeamServiceImpl();
		dflBest22Service = new DflBest22ServiceImpl();
		dflLadderService = new DflLadderServiceImpl();
		dflFixtureService = new DflFixtureServiceImpl();
		dflTeamScoresService = new DflTeamScoresServiceImpl();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round, String emailOverride) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "Executing end round for round={}",  round);
			
			if(emailOverride != null && !emailOverride.equals("")) {
				loggerUtils.log("info", "Overriding email with: {}", emailOverride);
				this.emailOverride = emailOverride;
			}
			
			MatthewAllenHandler matthewAllenHandler = new MatthewAllenHandler();
			matthewAllenHandler.configureLogging(mdcKey, loggerName, logfile);
			matthewAllenHandler.execute(round);
			List<DflMatthewAllen> matthewAllenStandings = dflMatthewAllenService.getForRound(round);
			Collections.sort(matthewAllenStandings, Collections.reverseOrder());
			
			AdamGoodesHandler adamGoodesHandler = new AdamGoodesHandler();
			adamGoodesHandler.configureLogging(mdcKey, loggerName, logfile);
			adamGoodesHandler.execute(round);
			List<DflAdamGoodes> adamGoodesStandings = adamGoodesHandler.getMedalStandings();
			List<DflAdamGoodes> topFirstYears = adamGoodesHandler.getTopFirstYears();
			
			CallumChambersHandler callumChambersHandler = new CallumChambersHandler();
			callumChambersHandler.configureLogging(mdcKey, loggerName, logfile);
			callumChambersHandler.execute(round);
			List<DflCallumChambers> callumChambersStandings = callumChambersHandler.getMedalStandings();
			
			Best22Handler best22Handler = new Best22Handler();
			best22Handler.configureLogging(mdcKey, loggerName, logfile);
			best22Handler.execute(round);
			List<DflBest22> best22 = dflBest22Service.getForRound(round);
			
			boolean sendBest22 = false;
			if(round == 6 || round == 12 || round == 18) {
				sendBest22 = true;
			}
			
			if(!globalsService.getSendMedalReports(round)) {
				createEndOfRoundEmail(round, matthewAllenStandings, adamGoodesStandings, topFirstYears, callumChambersStandings, best22, sendBest22);
			}
			
			switch(round) {
				case 18: calculateFinalsWeek1(round); break;
				case 19: calculateFinalsWeek2(round); break;
				case 20: calculateFinalsWeek3(round); break;
				case 21: calculateFinalsWeek4(round); break;
			}
			
			globalsService.setCurrentRound(round+1);
			
			dflMatthewAllenService.close();
			globalsService.close();
			dflPlayerService.close();
			dflTeamPlayerService.close();
			dflTeamService.close();	

			loggerUtils.log("info", "End round completed");
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
		
	}
	
	private void calculateFinalsWeek1(int round) throws Exception {
		
		loggerUtils.log("info", "Calculating fixture for week 1 of the finals....");
		
		List<DflLadder> ladder = dflLadderService.getLadderForRound(round);
		Collections.sort(ladder, Collections.reverseOrder());
		
		DflLadder first = ladder.get(0);
		DflLadder second = ladder.get(1);
		DflLadder third = ladder.get(2);
		DflLadder fourth = ladder.get(3);
		DflLadder fifth = ladder.get(4);
		
		DflFixture finalWeek1Game1 = new DflFixture();
		finalWeek1Game1.setRound(round+1);
		finalWeek1Game1.setGame(1);
		finalWeek1Game1.setHomeTeam(second.getTeamCode());
		finalWeek1Game1.setAwayTeam(third.getTeamCode());
		
		DflFixture finalWeek1Game2 = new DflFixture();
		finalWeek1Game2.setRound(round+1);
		finalWeek1Game2.setGame(2);
		finalWeek1Game2.setHomeTeam(fourth.getTeamCode());
		finalWeek1Game2.setAwayTeam(fifth.getTeamCode());
		
		List<DflFixture> finalsFixtures = new ArrayList<>();
		finalsFixtures.add(finalWeek1Game1);
		finalsFixtures.add(finalWeek1Game2);
		
		loggerUtils.log("info", "Week 1 finals fixtures={}", finalsFixtures);
		
		dflFixtureService.updateAll(finalsFixtures, false);
		
		createFinalsWeek1Email(round, finalWeek1Game1, finalWeek1Game2, first);
	}
	
	private void calculateFinalsWeek2(int round) throws Exception {
		
		loggerUtils.log("info", "Calculating fixture for week 2 of the finals....");
		
		DflFixturePK dflFixturePK = new DflFixturePK();
		dflFixturePK.setRound(round);
		dflFixturePK.setGame(1);
		DflFixture week1Game1 = dflFixtureService.get(dflFixturePK);
		
		dflFixturePK.setGame(2);
		DflFixture week1Game2 = dflFixtureService.get(dflFixturePK);
		
		String game1Winner;
		String game1Loser;
		
		DflTeamScoresPK dflTeamScorePK = new DflTeamScoresPK();
		dflTeamScorePK.setRound(round);
		dflTeamScorePK.setTeamCode(week1Game1.getHomeTeam());
		int homeTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		dflTeamScorePK.setTeamCode(week1Game1.getAwayTeam());
		int awayTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		if(homeTeamScore >= awayTeamScore) {
			game1Winner = week1Game1.getHomeTeam();
			game1Loser = week1Game1.getAwayTeam();
		} else {
			game1Winner = week1Game1.getAwayTeam();
			game1Loser = week1Game1.getHomeTeam();
		}
		
		String game2Winner;
		String game2Loser;
		
		dflTeamScorePK.setTeamCode(week1Game2.getHomeTeam());
		homeTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		dflTeamScorePK.setTeamCode(week1Game2.getAwayTeam());
		awayTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		if(homeTeamScore >= awayTeamScore) {
			game2Winner = week1Game2.getHomeTeam();
			game2Loser = week1Game2.getAwayTeam();
		} else {
			game2Winner = week1Game2.getAwayTeam();
			game2Loser = week1Game2.getHomeTeam();
		}
		
		List<DflLadder> ladder = dflLadderService.getLadderForRound(round - 1);
		Collections.sort(ladder, Collections.reverseOrder());
		
		DflLadder first = ladder.get(0);
		
		DflFixture finalWeek2Game1 = new DflFixture();
		finalWeek2Game1.setRound(round+1);
		finalWeek2Game1.setGame(1);
		finalWeek2Game1.setHomeTeam(first.getTeamCode());
		finalWeek2Game1.setAwayTeam(game1Winner);
		
		DflFixture finalWeek2Game2 = new DflFixture();
		finalWeek2Game2.setRound(round+1);
		finalWeek2Game2.setGame(2);
		finalWeek2Game2.setHomeTeam(game1Loser);
		finalWeek2Game2.setAwayTeam(game2Winner);
		
		List<DflFixture> finalsFixtures = new ArrayList<>();
		finalsFixtures.add(finalWeek2Game1);
		finalsFixtures.add(finalWeek2Game2);
		
		loggerUtils.log("info", "Week 2 finals fixtures={}", finalsFixtures);
		
		dflFixtureService.updateAll(finalsFixtures, false);
		
		createFinalsWeek2Email(round, finalWeek2Game1, finalWeek2Game2, game2Loser);
	}
	
	private void calculateFinalsWeek3(int round) throws Exception {
		
		loggerUtils.log("info", "Calculating fixture for week 3 of the finals....");
		
		DflFixturePK dflFixturePK = new DflFixturePK();
		dflFixturePK.setRound(round);
		dflFixturePK.setGame(1);
		DflFixture week2Game1 = dflFixtureService.get(dflFixturePK);
		
		dflFixturePK.setGame(2);
		DflFixture week2Game2 = dflFixtureService.get(dflFixturePK);
		
		String game1Loser;
		
		DflTeamScoresPK dflTeamScorePK = new DflTeamScoresPK();
		dflTeamScorePK.setRound(round);
		dflTeamScorePK.setTeamCode(week2Game1.getHomeTeam());
		int homeTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		dflTeamScorePK.setTeamCode(week2Game1.getAwayTeam());
		int awayTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		if(homeTeamScore >= awayTeamScore) {
			game1Loser = week2Game1.getAwayTeam();
		} else {
			game1Loser = week2Game1.getHomeTeam();
		}
		
		String game2Winner;
		String game2Loser;
		
		dflTeamScorePK.setTeamCode(week2Game2.getHomeTeam());
		homeTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		dflTeamScorePK.setTeamCode(week2Game2.getAwayTeam());
		awayTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		if(homeTeamScore >= awayTeamScore) {
			game2Winner = week2Game2.getHomeTeam();
			game2Loser = week2Game2.getAwayTeam();
		} else {
			game2Winner = week2Game2.getAwayTeam();
			game2Loser = week2Game2.getHomeTeam();
		}
		
		DflFixture finalWeek3Game1 = new DflFixture();
		finalWeek3Game1.setRound(round+1);
		finalWeek3Game1.setGame(1);
		finalWeek3Game1.setHomeTeam(game1Loser);
		finalWeek3Game1.setAwayTeam(game2Winner);
		
		List<DflFixture> finalsFixtures = new ArrayList<>();
		finalsFixtures.add(finalWeek3Game1);
		
		loggerUtils.log("info", "Week 3 finals fixtures={}", finalsFixtures);
		
		dflFixtureService.updateAll(finalsFixtures, false);
		
		createFinalsWeek3Email(round, finalWeek3Game1, game2Loser);		
	}
	
	private void calculateFinalsWeek4(int round) throws Exception {
		
		loggerUtils.log("info", "Calculating fixture for week 4 of the finals....");
		
		DflFixturePK dflFixturePK = new DflFixturePK();
		dflFixturePK.setRound(round);
		dflFixturePK.setGame(1);
		DflFixture week3Game1 = dflFixtureService.get(dflFixturePK);
				
		String game1Winner;
		String game1Loser;
		
		DflTeamScoresPK dflTeamScorePK = new DflTeamScoresPK();
		dflTeamScorePK.setRound(round);
		dflTeamScorePK.setTeamCode(week3Game1.getHomeTeam());
		int homeTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		dflTeamScorePK.setTeamCode(week3Game1.getAwayTeam());
		int awayTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		if(homeTeamScore >= awayTeamScore) {
			game1Winner = week3Game1.getHomeTeam();
			game1Loser = week3Game1.getAwayTeam();
		} else {
			game1Winner = week3Game1.getAwayTeam();
			game1Loser = week3Game1.getHomeTeam();
		}
		
		dflFixturePK = new DflFixturePK();
		dflFixturePK.setRound(round - 1);
		dflFixturePK.setGame(1);
		DflFixture week2Game1 = dflFixtureService.get(dflFixturePK);
				
		String week2Winner;
		
		dflTeamScorePK = new DflTeamScoresPK();
		dflTeamScorePK.setRound(round - 1);
		dflTeamScorePK.setTeamCode(week2Game1.getHomeTeam());
		homeTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		dflTeamScorePK.setTeamCode(week2Game1.getAwayTeam());
		awayTeamScore = dflTeamScoresService.get(dflTeamScorePK).getScore();
		
		if(homeTeamScore >= awayTeamScore) {
			week2Winner = week2Game1.getHomeTeam();
		} else {
			week2Winner = week2Game1.getAwayTeam();
		}
				
		DflFixture finalWeek4Game1 = new DflFixture();
		finalWeek4Game1.setRound(round+1);
		finalWeek4Game1.setGame(1);
		finalWeek4Game1.setHomeTeam(week2Winner);
		finalWeek4Game1.setAwayTeam(game1Winner);
		
		List<DflFixture> finalsFixtures = new ArrayList<>();
		finalsFixtures.add(finalWeek4Game1);
		
		loggerUtils.log("info", "Week 4 finals fixtures={}", finalsFixtures);
		
		dflFixtureService.updateAll(finalsFixtures, false);
		
		createFinalsWeek4Email(round, finalWeek4Game1, game1Loser);
	}
	
	private void createEndOfRoundEmail(int round, List<DflMatthewAllen> matthewAllenStandings, List<DflAdamGoodes> adamGoodesStandings,
				 					List<DflAdamGoodes> topFirstYears, List<DflCallumChambers> callumChambersStandings, List<DflBest22> best22, boolean sendBest22) throws Exception {
		
		loggerUtils.log("info", "Creating email for end of round - Round: {}", round);
		
		String subject = "End of Round " + round + ", Current Medal Standings";
		
		String body = "Matthew Allen Medal Top 5:\n";
		for(int i = 0; i < 5; i++) {
			DflMatthewAllen standing = matthewAllenStandings.get(i);
			
			DflPlayer player = dflPlayerService.get(standing.getPlayerId());
			DflTeamPlayer teamPlayer = dflTeamPlayerService.get(standing.getPlayerId());
			DflTeam team = dflTeamService.get(teamPlayer.getTeamCode());
			
			body = body + (i+1) + ". " + standing.getPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + ", " + team.getName() + " - " + standing.getTotal() + "\n";
		}
		
		body = body + "\nAdam Goodes Medal Top 5:\n";
		for(int i = 0; i < 5; i++) {
			if(i < adamGoodesStandings.size()) {
				DflAdamGoodes standing = adamGoodesStandings.get(i);
				
				DflPlayer player = dflPlayerService.get(standing.getPlayerId());
				DflTeamPlayer teamPlayer = dflTeamPlayerService.get(standing.getPlayerId());
				DflTeam team = dflTeamService.get(teamPlayer.getTeamCode());
				
				body = body + (i+1) + ". " + standing.getPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + ", " + team.getName() + " - " + standing.getTotalScore() + "\n";
			}
		}
		
		body = body + "\nFirst Year Player Top 5:\n";
		for(int i = 0; i < 5; i++) {
			if(i < topFirstYears.size()) {
				DflAdamGoodes standing = topFirstYears.get(i);
				
				DflPlayer player = dflPlayerService.get(standing.getPlayerId());
				DflTeamPlayer teamPlayer = dflTeamPlayerService.get(standing.getPlayerId());
				
				if(teamPlayer != null) {
					DflTeam team = dflTeamService.get(teamPlayer.getTeamCode());
					body = body + (i+1) + ". " + standing.getPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + ", " + team.getName() + " - " + standing.getTotalScore() + "\n";
				} else {
					body = body + (i+1) + ". " + standing.getPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + ", Not Drafted - " + standing.getTotalScore() + "\n";
				}	
			}
		}
		
		body = body + "\nCallum Chambers Medal Top 5:\n";
		for(int i = 0; i < 5; i++) {
			DflCallumChambers standing = callumChambersStandings.get(i);
			
			DflPlayer player = dflPlayerService.get(standing.getPlayerId());
			DflTeamPlayer teamPlayer = dflTeamPlayerService.get(standing.getPlayerId());
			DflTeam team = dflTeamService.get(teamPlayer.getTeamCode());
			
			body = body + (i+1) + ". " + standing.getPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + ", " + team.getName() + " - " + standing.getTotalScore() + "\n";
		}
		
		if(sendBest22) {
			body = body + "\nDFL Best 22 after Round " + round + "\n";
			
			List<String> ff = new  ArrayList<>();
			List<String> fwd = new  ArrayList<>();
			List<String> rck = new  ArrayList<>();
			List<String> mid = new  ArrayList<>();
			List<String> fb = new  ArrayList<>();
			List<String> def = new  ArrayList<>();
			List<String> bench = new  ArrayList<>();
			
			for(DflBest22 best22Player : best22) {
				DflPlayer player = dflPlayerService.get(best22Player.getPlayerId());
				DflTeamPlayer teamPlayer = dflTeamPlayerService.get(best22Player.getPlayerId());
				DflTeam team = dflTeamService.get(teamPlayer.getTeamCode());
				if(best22Player.isBench()) {
					bench.add(player.getPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + ", " + team.getName() + " - " + best22Player.getScore());
				} else {
					String displayString = player.getPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + ", " + team.getName() + " - " + best22Player.getScore();
					switch(player.getPosition()) {
						case "FF": ff.add(displayString); break;
						case "Fwd": fwd.add(displayString); break;
						case "Rck": rck.add(displayString); break;
						case "Mid": mid.add(displayString); break;
						case "FB": fb.add(displayString); break;
						case "Def": def.add(displayString); break;
					}
				}
			}
			
			body = body + "FB:\n";
			for(String displayString : fb) {
				body = body + displayString + "\n";
			}
			body = body + "\nDef:\n";
			for(String displayString : def) {
				body = body + displayString + "\n";
			}
			body = body + "\nRck:\n";
			for(String displayString : rck) {
				body = body + displayString + "\n";
			}
			body = body + "\nMid:\n";
			for(String displayString : mid) {
				body = body + displayString + "\n";
			}
			body = body + "\nFwd:\n";
			for(String displayString : fwd) {
				body = body + displayString + "\n";
			}
			body = body + "\nFF:\n";
			for(String displayString : ff) {
				body = body + displayString + "\n";
			}
			body = body + "\nBench:\n";
			for(String displayString : bench) {
				body = body + displayString + "\n";
			}
		}
				
		body = body + "\nDFL Manager Admin";
		
		sendEmail(subject, body);
	}
	
	private void createFinalsWeek1Email(int round, DflFixture finalsGame1, DflFixture finalsGame2, DflLadder first) throws Exception {
		
		loggerUtils.log("info", "Creating email for finals week 1 - Game 1: {}, Game 2 {}, Minor Premier: {}", finalsGame1, finalsGame2, first.getTeamCode());
				
		String subject = "Finals Week 1";
		
		DflTeam dflTeam = dflTeamService.get(first.getTeamCode());
		
		String body = "Congratulations to " + dflTeam.getName() + " and " + dflTeam.getCoachName() + "for winning the minor premiership\n\n";
		body = body + "The fixture for week 1 of the finals is:\n\n";
		
		DflTeam homeTeam = dflTeamService.get(finalsGame1.getHomeTeam());
		DflTeam awayTeam = dflTeamService.get(finalsGame1.getAwayTeam());
		
		body = body + "\tQualifying Final: " + homeTeam.getShortName() + " vs " + awayTeam.getShortName() + "\n";
		
		homeTeam = dflTeamService.get(finalsGame2.getHomeTeam());
		awayTeam = dflTeamService.get(finalsGame2.getAwayTeam());
		
		body = body + "\tElimination Final: " + homeTeam.getShortName() + " vs " + awayTeam.getShortName() + "\n\n";
		
		body = body + "Good luck,\nDFL Manager Admin";
		
		sendEmail(subject, body);
	}
	
	private void createFinalsWeek2Email(int round, DflFixture finalsGame1, DflFixture finalsGame2, String out) throws Exception {
		
		loggerUtils.log("info", "Creating email for finals week 2 - Game 1: {}, Game 2 {}, Out: {}", finalsGame1, finalsGame2, out);
				
		String subject = "Finals Week 2";
		
		DflTeam dflTeam = dflTeamService.get(out);
		
		String body = dflTeam.getName() + " has been eliminated. Better luck next year " + dflTeam.getCoachName() + "\n\n";
		body = body + "The fixture for week 2 of the finals is:\n\n";
		
		DflTeam homeTeam = dflTeamService.get(finalsGame1.getHomeTeam());
		DflTeam awayTeam = dflTeamService.get(finalsGame1.getAwayTeam());
		
		body = body + "\t2nd Semi Final: " + homeTeam.getShortName() + " vs " + awayTeam.getShortName() + "\n";
		
		homeTeam = dflTeamService.get(finalsGame2.getHomeTeam());
		awayTeam = dflTeamService.get(finalsGame2.getAwayTeam());
		
		body = body + "\t1st Semi Final: " + homeTeam.getShortName() + " vs " + awayTeam.getShortName() + "\n\n";
		
		body = body + "Good luck,\nDFL Manager Admin";
		
		sendEmail(subject, body);
	}
	
	private void createFinalsWeek3Email(int round, DflFixture finalsGame1, String out) throws Exception {
		
		loggerUtils.log("info", "Creating email for finals week 3 - Game 1: {}, Out: {}", finalsGame1, out);
				
		String subject = "Finals Week 3";
		
		DflTeam dflTeam = dflTeamService.get(out);
		
		String body = dflTeam.getName() + " has been eliminated. Better luck next year " + dflTeam.getCoachName() + "\n\n";
		body = body + "The fixture for week 3 of the finals is:\n\n";
		
		DflTeam homeTeam = dflTeamService.get(finalsGame1.getHomeTeam());
		DflTeam awayTeam = dflTeamService.get(finalsGame1.getAwayTeam());
		
		body = body + "\tPreliminary Final: " + homeTeam.getShortName() + " vs " + awayTeam.getShortName() + "\n";
		
		body = body + "Good luck,\nDFL Manager Admin";
		
		sendEmail(subject, body);
	}

	private void createFinalsWeek4Email(int round, DflFixture finalsGame1, String out) throws Exception {
		
		loggerUtils.log("info", "Creating email for finals week 4 - Game 1: {}, Out: {}", finalsGame1, out);
				
		String subject = "DFL Grand Final";
		
		DflTeam dflTeam = dflTeamService.get(out);
		
		String body = dflTeam.getName() + " has been eliminated. Better luck next year " + dflTeam.getCoachName() + "\n\n";
		body = body + "The fixture for the grand final is:\n\n";
		
		DflTeam homeTeam = dflTeamService.get(finalsGame1.getHomeTeam());
		DflTeam awayTeam = dflTeamService.get(finalsGame1.getAwayTeam());
		
		body = body + "\tGrand Final: " + homeTeam.getShortName() + " vs " + awayTeam.getShortName() + "\n";
		
		body = body + "Good luck,\nDFL Manager Admin";
		
		sendEmail(subject, body);
	}
	
	private void sendEmail(String subject, String body) throws Exception {
		
		String dflMngrEmail = globalsService.getEmailConfig().get("dflmngrEmailAddr");
		
		List<String> to = new ArrayList<>();

		if(emailOverride != null && !emailOverride.equals("")) {
			to.add(emailOverride);
		} else {
			List<DflTeam> teams = dflTeamService.findAll();
			for(DflTeam team : teams) {
				to.add(team.getCoachEmail());
			}
		}
		
		loggerUtils.log("info", "Emailing to={};", to);
		EmailUtils.sendTextEmail(to, dflMngrEmail, subject, body, null);
	}
	
	
	public static void main(String[] args) {
		
		Options options = new Options();
		Option roundOpt  = Option.builder("r").argName("round").hasArg().desc("round to run on").type(Number.class).required().build();
		Option emailOPt = Option.builder("e").argName("email").hasArg().desc("override email distribution").build();
		options.addOption(roundOpt);
		options.addOption(emailOPt);
		
		try {
			int round = 0;
			String email = null;
						
			CommandLineParser parser = new DefaultParser();
			CommandLine cli = parser.parse(options, args);
			
			round = ((Number)cli.getParsedOptionValue("r")).intValue();
			
			if(cli.hasOption("e")) {
				email = cli.getOptionValue("e");
			}
		
			//JndiProvider.bind();
			
			EndRoundHandler endRound = new EndRoundHandler();
			endRound.configureLogging("batch.name", "batch-logger", ("EndRound_R" + round));
			endRound.execute(round, email);
		} catch (ParseException ex) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "MatthewAllenHandler", options );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
