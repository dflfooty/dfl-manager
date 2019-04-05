package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflFixture;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.service.DflFixtureService;
import net.dflmngr.model.service.DflTeamPredictedScoresService;
import net.dflmngr.model.service.DflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.DflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPredictedScoresServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.utils.EmailUtils;

public class StartRoundHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "StartRound";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	DflTeamService dflTeamService;
	GlobalsService globalsService;
	DflFixtureService dflFixtureService;
	DflTeamPredictedScoresService dflTeamPredictedScoresService;
	
	String emailOverride;
	
	public StartRoundHandler() {
		dflTeamService = new DflTeamServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflFixtureService = new DflFixtureServiceImpl();
		dflTeamPredictedScoresService = new DflTeamPredictedScoresServiceImpl();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round, String emailOveride, boolean fromScoresCalculator) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			if(emailOveride != null && !emailOveride.equals("")) {
				this.emailOverride = emailOveride;
			}
			
			if(!fromScoresCalculator) {
				sendFirstGameEmail(round, emailOveride);
			}
			
			loggerUtils.log("info", "Start round completed");
			
			dflTeamService.close();
			globalsService.close();
			dflFixtureService.close();
			dflTeamPredictedScoresService.close();
		
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
		
	private void sendFirstGameEmail(int round, String emailOveride) throws Exception {
		
		String dflMngrEmail = globalsService.getEmailConfig().get("dflmngrEmailAddr");
		
		String subject = "DFL Manager - Predictions";
		
		List<DflFixture> roundFixtures = dflFixtureService.getFixturesForRound(round);
		
		String body = "<html>\n<body>\n";
		body = "<p>This week in round " + round + "</p>\n";
		body = body + "<p>DFL Manager has made the following predictions:</p>\n";
		body = body + "<p><ul type=none>\n";
						
		for(DflFixture fixture : roundFixtures) {
			DflTeam homeTeam = dflTeamService.get(fixture.getHomeTeam());
			int homeTeamPredictedScore = dflTeamPredictedScoresService.getTeamPredictedScoreForRound(homeTeam.getTeamCode(), round).getPredictedScore();
			
			DflTeam awayTeam = dflTeamService.get(fixture.getAwayTeam());
			int awayTeamPredictedScore = dflTeamPredictedScoresService.getTeamPredictedScoreForRound(awayTeam.getTeamCode(), round).getPredictedScore();
			
			String resultString = "";
			if(homeTeamPredictedScore > awayTeamPredictedScore) {
				resultString = " to defeat ";
			} else {
				resultString = " to be defeated by ";
			}
			
			String gameUrl = globalsService.getOnlineBaseUrl() + "/results/" + fixture.getRound() + "/" + fixture.getGame();
			
			body = body + "<li>" + homeTeam.getName() + " " + resultString + awayTeam.getName() + ", " + homeTeamPredictedScore + " to " + awayTeamPredictedScore + 
				   " - <a herf='" + gameUrl + "'>Match Report</a></li>\n";
		}
		
		body = body + "</ul></p>\n";
		
		body = body + "<p>DFL Manager Admin</p>\n";
		body = body + "</body>\n</html>";
		
		List<DflTeam> teams = dflTeamService.findAll();
		List<String> to = new ArrayList<>();
		
		if(emailOverride != null && !emailOverride.equals("")) {
			to.add(emailOverride);
		} else {
			for(DflTeam team : teams) {
				to.add(team.getCoachEmail());
			}
		}

		loggerUtils.log("info", "Emailing early games start round to={}", to);
		EmailUtils.sendHtmlEmail(to, dflMngrEmail, subject, body, null);	
	}
	
	public static void main(String[] args) {
		
		try {
			String email = null;
			int round = 0;
			
			if(args.length > 2 || args.length < 1) {
				System.out.println("usage: RawStatsReport <round> optional [<email>]");
			} else {
				
				round = Integer.parseInt(args[0]);
				
				if(args.length == 2) {
					email = args[1];
				}
				
				StartRoundHandler startRound = new StartRoundHandler();
				startRound.configureLogging("batch.name", "batch-logger", "StartRound");
				startRound.execute(round, email, false);
			}
			
			System.exit(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
