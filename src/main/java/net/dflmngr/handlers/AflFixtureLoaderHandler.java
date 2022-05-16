package net.dflmngr.handlers;

import java.time.format.DateTimeFormatter;
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
	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd h:mm a yyyy");
	
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
			loggerUtils.log("error", "Error in ... ", ex);
		}	
	}
	
	public void execute(List<Integer> aflRounds) throws Exception {
		
		try {
			loggerUtils.log("info", "Executing AflFixtureLoader for rounds: {}", aflRounds);
			
			List<AflFixture> allGames = new ArrayList<>();

			List<String> aflFixtureUrlParts = globalsService.getAflFixtureUrl();
			
			for(Integer aflRound : aflRounds) {
				String aflFixtureUrl = aflFixtureUrlParts.get(0) + aflRound;
				allGames.addAll(aflFixtureHtmlHandler.execute(aflRound, aflFixtureUrl));
			}
			
			loggerUtils.log("info", "Saveing data to DB");
			
			aflFixtureService.updateLoadedFixtures(allGames);
			
			loggerUtils.log("info", "AflFixtureLoader Complete");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
		
	// For internal testing
	public static void main(String[] args) {

		Options options = new Options();
		Option all = new Option("all", "All rounds");
		options.addOption(all);

		try {

			boolean allRounds = false;

			CommandLineParser parser = new DefaultParser();
			CommandLine cli = parser.parse(options, args);
			
			if(cli.hasOption("all")) {
				allRounds = true;
			}

			AflFixtureLoaderHandler testing = new AflFixtureLoaderHandler();				
			List<Integer> testRounds = new ArrayList<>();
			
			if(allRounds) {
				for(int i = 1; i < 24; i++) {
					testRounds.add(i);
				}
			} else {
				int startAtRound = Integer.parseInt(testing.globalsService.getCurrentRound()) + 1;
				for(int i = startAtRound; i < 24; i++) {
					testRounds.add(i);
				}
			}
			
			testing.execute(testRounds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}