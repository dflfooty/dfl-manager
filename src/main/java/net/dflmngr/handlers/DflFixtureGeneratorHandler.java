package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflFixture;
import net.dflmngr.model.service.DflFixtureService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.DflFixtureServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class DflFixtureGeneratorHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "DflFixtureGeneratorHandler";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	private GlobalsService globalsService;
	private DflFixtureService dflFixtureService;
	
	public DflFixtureGeneratorHandler() {
		globalsService = new GlobalsServiceImpl();
		dflFixtureService = new DflFixtureServiceImpl();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute() {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "DflFixtureGeneratorHandler excuting....");
			
			loggerUtils.log("info", "Generating DFL Fixture");
			generateFixture();
						
			loggerUtils.log("info", "DflFixtureGeneratorHandler completed");
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private void generateFixture() {
		
		loggerUtils.log("info", "Fetching Global Data");
		Map<Integer, Map<Integer, String[]>> dflFixtureTemplate = globalsService.getDflFixuteTemplate();
		Map<String, String> dlfFixtureOrder = globalsService.getDflFixtureOrder();
		
		List<DflFixture> dflFixture = new ArrayList<>();
		
		for (Map.Entry<Integer,  Map<Integer, String[]>> roundEntry : dflFixtureTemplate.entrySet()) {
		    int round = roundEntry.getKey();
		    Map<Integer, String[]> roundTemplate = roundEntry.getValue();
		    
		    for (Map.Entry<Integer, String[]> gameEntry : roundTemplate.entrySet()) {
			    int game = gameEntry.getKey();
			    String[] teams = gameEntry.getValue();
			    
			    String homeTeamIndex = teams[0];
			    String awayTeamIndex = teams[1];
			    
			    String homeTeam = dlfFixtureOrder.get(homeTeamIndex);
			    String awayTeam = dlfFixtureOrder.get(awayTeamIndex);
			    
			    DflFixture dflGame = new DflFixture();
			    dflGame.setRound(round);
			    dflGame.setGame(game);
			    dflGame.setHomeTeam(homeTeam);
			    dflGame.setAwayTeam(awayTeam);
			    
			    loggerUtils.log("info", "Creating DFL fixture game: {}", dflGame);
			    dflFixture.add(dflGame);
		    }
		}
		
		loggerUtils.log("info", "Saving fixutres in db...");
		dflFixtureService.replaceAll(dflFixture);
		
		loggerUtils.log("info", "DFl fixture generated");
	}
	
	
	public static void main(String[] args) {		
		try {
			
			DflFixtureGeneratorHandler fixuteGenerator = new DflFixtureGeneratorHandler();
			fixuteGenerator.configureLogging("batch.name", "batch-logger", "DflFixtureGenerator");
			fixuteGenerator.execute();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
