package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.AflFixture;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.AflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.AflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class AflFixtureLoaderHandler {
	private LoggingUtils loggerUtils;
		
	GlobalsService globalsService;
	AflFixtureService aflFixtureService;
	AflTeamService aflTeamService;
	AflFixtureHtmlHandler aflFixtureHtmlHandler;
	
	public AflFixtureLoaderHandler() {
		
		loggerUtils = new LoggingUtils("AflFixtureLoader");
		
		try {
			globalsService = new GlobalsServiceImpl();
			aflFixtureService = new AflFixtureServiceImpl();
			aflTeamService = new AflTeamServiceImpl();
			aflFixtureHtmlHandler = new AflFixtureHtmlHandler();
		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}	
	}
	
	public void execute(List<Integer> aflRounds) {

		loggerUtils.log("info", "Executing AflFixtureLoader for rounds: {}", aflRounds);
		
		List<AflFixture> allGames = new ArrayList<>();

		List<String> aflFixtureUrlParts = globalsService.getAflFixtureUrl();
		int aflRoundBaseUri = Integer.parseInt(aflFixtureUrlParts.get(1));
		
		for(Integer aflRound : aflRounds) {
			int aflRoundUri = aflRoundBaseUri + aflRound;
			String aflFixtureUrl = aflFixtureUrlParts.get(0) + aflRoundUri;
			allGames.addAll(aflFixtureHtmlHandler.execute(aflRound, aflFixtureUrl));
		}
		
		loggerUtils.log("info", "Saveing data to DB");
		
		aflFixtureService.updateLoadedFixtures(allGames);
		
		loggerUtils.log("info", "AflFixtureLoader Complete");
	}
		
	// For internal testing
	public static void main(String[] args) {

		Options options = new Options();
		Option all = new Option("all", "All rounds");
		Option startRound = Option.builder("s")
							.argName("start")
							.hasArg()
							.desc("Round to start fixture scraping")
							.type(Number.class)
							.build();
		Option endRound = Option.builder("e")
							.argName("end")
							.hasArg()
							.desc("Round to end fixture scraping")
							.type(Number.class)
							.build();
		options.addOption(all);
		options.addOption(startRound);
		options.addOption(endRound);

		try {

			CommandLineParser parser = new DefaultParser();
			CommandLine cli = parser.parse(options, args);
			
			AflFixtureLoaderHandler testing = new AflFixtureLoaderHandler();				
			List<Integer> testRounds = new ArrayList<>();
			
			if(cli.hasOption("all")) {
				for(int i = 0; i <= 24; i++) {
					testRounds.add(i);
				}
			} else if(cli.hasOption("s")) {
				int startAtRound = ((Number) cli.getParsedOptionValue("s")).intValue();
				int endAtRound = 24;
				if(cli.hasOption("e")) {
					endAtRound = ((Number) cli.getParsedOptionValue("e")).intValue();
				}
				for(int i = startAtRound; i <= endAtRound; i++) {
					testRounds.add(i);
				}
			} else {
				int startAtRound = testing.aflFixtureService.getRefreshFixtureStart();
				for(int i = startAtRound; i <= 24; i++) {
					testRounds.add(i);
				}
			}
			
			testing.execute(testRounds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}