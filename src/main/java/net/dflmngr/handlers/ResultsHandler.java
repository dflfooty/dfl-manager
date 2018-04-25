package net.dflmngr.handlers;

import java.util.ArrayList;
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
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.reports.ResultsReport;

public class ResultsHandler {
	private LoggingUtils loggerUtils;
	
	AflFixtureService aflFixtureService;
	DflRoundInfoService dflRoundInfoService;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "RoundProgress";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	String emailOverride;
	
	public ResultsHandler() {
		aflFixtureService = new AflFixtureServiceImpl();
		dflRoundInfoService = new DflRoundInfoServiceImpl();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
		emailOverride = null;
	}
	
	public void execute(int inputRound, boolean isFinal, String emailOverride, boolean skipStats, boolean onHeroku, boolean sendReport) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "ResultsHandler excuting ....");

			if(emailOverride != null && !emailOverride.equals("")) {
				loggerUtils.log("info", "Overriding email with: {}", emailOverride);
				this.emailOverride = emailOverride;
			}
			
			if(!isFinal) {
				loggerUtils.log("info", "Completing AFL games");
				AflGameCompletionCheckerHandler gameCompletor = new AflGameCompletionCheckerHandler();
				gameCompletor.configureLogging(mdcKey, loggerName, logfile);
				gameCompletor.execute();
			}
			
			List<Integer> roundsToProcess = new ArrayList<>();
			if(inputRound == 0) {
				loggerUtils.log("info", "Check for multiple rounds");
				List<Integer> aflRounds = aflFixtureService.getAflRoundsToScrape();
				List<DflRoundInfo> dflRoundsInfo = dflRoundInfoService.findAll();
				
				for(DflRoundInfo roundInfo : dflRoundsInfo) {
					for(DflRoundMapping roundMapping : roundInfo.getRoundMapping()) {
						if(aflRounds.contains(roundMapping.getAflRound()) && !roundsToProcess.contains(roundMapping.getRound())) {
							roundsToProcess.add(roundMapping.getRound());
						}
					}
				}
			} else {
				loggerUtils.log("info", "Using specfic round");
				roundsToProcess.add(inputRound);
			}
			
			loggerUtils.log("info", "Rounds to process, rounds={} ....", roundsToProcess);
			
			for(int round : roundsToProcess) {
				loggerUtils.log("info", "Handling round={} ....", round);
				
				if(!skipStats) {
					loggerUtils.log("info", "Getting stats");
					RawPlayerStatsHandler statsHandler = new RawPlayerStatsHandler();
					statsHandler.configureLogging(mdcKey, loggerName, logfile);
					statsHandler.execute(round, isFinal, onHeroku);
				}
	
				loggerUtils.log("info", "Calculating scores");
				ScoresCalculatorHandler scoresCalculator = new ScoresCalculatorHandler();
				scoresCalculator.configureLogging(mdcKey, loggerName, logfile);
				scoresCalculator.execute(round);
				
				if(isFinal) {
					loggerUtils.log("info", "Calculating Ladder");
					LadderCalculatorHandler ladderCalculator = new LadderCalculatorHandler();
					ladderCalculator.configureLogging(mdcKey, loggerName, logfile);
					ladderCalculator.execute(round, false, null);
				}
				
				if(sendReport) {
					loggerUtils.log("info", "Writing report");
					ResultsReport resultsReport = new ResultsReport();
					resultsReport.configureLogging(mdcKey, loggerName, logfile);
					resultsReport.execute(round, isFinal, emailOverride);
				}
				
				loggerUtils.log("info", "Handled round={} ....", round);
			}
			
			loggerUtils.log("info", "ResultsHandler complete");

		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	public static void main(String[] args) {
		
		Options options = new Options();
		
		Option roundOpt  = Option.builder("r").argName("round").hasArg().desc("round to run on").type(Number.class).required().build();
		Option emailOPt = Option.builder("e").argName("email").hasArg().desc("override email distribution").build();
		Option finalOpt = new Option("f", "final run");
		Option skipStatsOpt = new Option("ss", "skip stats download");
		Option onHerokuOpt = new Option("h", "running on Heroku");
		Option sendReportOpt = new Option("rp", "send report");
		
		options.addOption(roundOpt);
		options.addOption(emailOPt);
		options.addOption(finalOpt);
		options.addOption(skipStatsOpt);
		options.addOption(onHerokuOpt);
		options.addOption(sendReportOpt);
		
		try {
			String email = null;
			int round = 0;
			boolean isFinal = false;
			boolean skipStats = false;
			boolean onHeroku = false;
			boolean sendReport = false;
						
			CommandLineParser parser = new DefaultParser();
			CommandLine cli = parser.parse(options, args);
			
			round = ((Number)cli.getParsedOptionValue("r")).intValue();
			
			if(cli.hasOption("e")) {
				email = cli.getOptionValue("e");
			}
			if(cli.hasOption("f")) {
				isFinal = true;
			}
			if(cli.hasOption("ss")) {
				skipStats=true;
			}
			if(cli.hasOption("h")) {
				onHeroku = true;
			}
			if(cli.hasOption("rp")) {
				sendReport = true;
			}

			//JndiProvider.bind();
			
			ResultsHandler resultsHandler = new ResultsHandler();
			resultsHandler.configureLogging("batch.name", "batch-logger", ("ResultsHandler_R" + round));
			resultsHandler.execute(round, isFinal, email, skipStats, onHeroku, sendReport);
			
			System.exit(0);

		} catch (ParseException ex) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "RawStatsReport", options );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
