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

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflRoundInfo;
import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.reports.ResultsReport;

public class ResultsHandler {
	private LoggingUtils loggerUtils;
	
	AflFixtureService aflFixtureService;
	DflRoundInfoService dflRoundInfoService;
	GlobalsService globalsService;
	
	boolean isExecutable;
	String defaultLogfile = "RoundProgress";
	String logfile;
	
	String emailOverride;
	
	public ResultsHandler() {
		aflFixtureService = new AflFixtureServiceImpl();
		dflRoundInfoService = new DflRoundInfoServiceImpl();
		globalsService = new GlobalsServiceImpl();
	}
	
	public void configureLogging(String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.logfile = logfile;
		isExecutable = true;
		emailOverride = null;
	}
	
	public void execute(int inputRound, boolean isFinal, String emailOverride, boolean skipStats, boolean sendReport) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "ResultsHandler excuting ....");

			if(emailOverride != null && !emailOverride.equals("")) {
				loggerUtils.log("info", "Overriding email with: {}", emailOverride);
				this.emailOverride = emailOverride;
			}
			
			comppleteAflGames(isFinal);
			List<Integer> roundsToProcess = getRoundsToProcess(inputRound);
			
			for(int round : roundsToProcess) {
				loggerUtils.log("info", "Handling round={} ....", round);

				getStats(inputRound, round, skipStats);
				calcuateScores(round);
				calculateLadder(round, isFinal);
				sendReport(round, isFinal, sendReport);
				
				loggerUtils.log("info", "Handled round={} ....", round);
			}

			aflFixtureService.close();
			dflRoundInfoService.close();
			globalsService.close();
			
			loggerUtils.log("info", "ResultsHandler complete");

		} catch (Exception ex) {
			loggerUtils.logException("Error in ... ", ex);
		}
	}

	private List<Integer> getRoundsToProcess(int round) {
		List<Integer> roundsToProcess = new ArrayList<>();
		if(round == 0) {
			loggerUtils.log("info", "Check for multiple rounds");
			List<Integer> aflRounds = aflFixtureService.getAflRoundsToScrape();
			List<DflRoundInfo> dflRoundsInfo = dflRoundInfoService.findAll();
			
			loggerUtils.log("info", "Inprogress AFL rounds {}", aflRounds);
			
			if(!isStatsRoundOnly(aflRounds)) {
				for(DflRoundInfo roundInfo : dflRoundsInfo) {
					for(DflRoundMapping roundMapping : roundInfo.getRoundMapping()) {
						if(aflRounds.contains(roundMapping.getAflRound()) && !roundsToProcess.contains(roundMapping.getRound())) {
							roundsToProcess.add(roundMapping.getRound());
						}
					}
				}
			}
		} else {
			loggerUtils.log("info", "Using specfic round");
			roundsToProcess.add(round);
		}

		loggerUtils.log("info", "Rounds to process, rounds={} ....", roundsToProcess);
		return roundsToProcess;
	}

	private boolean isStatsRoundOnly(List<Integer> aflRounds) {
		boolean isStatsRound = false;

		if(aflRounds.size() == 1) {
			List<Integer> statRounds = globalsService.getStatRounds();
			isStatsRound = statRounds.contains(aflRounds.get(0));
		}		

		loggerUtils.log("info", "Stats AFL round only: {} AFL rounds: {}", isStatsRound, aflRounds);
		return isStatsRound;
	}

	private void comppleteAflGames(boolean isFinal) {
		if(!isFinal) {
			loggerUtils.log("info", "Completing AFL games");
			AflGameCompletionCheckerHandler gameCompletor = new AflGameCompletionCheckerHandler();
			gameCompletor.configureLogging(logfile);
			gameCompletor.execute();
		}
	}

	private void getStats(int inputRound, int round, boolean skipStats) {
		if(!skipStats) {
			loggerUtils.log("info", "Getting stats");
			RawPlayerStatsHandler statsHandler = new RawPlayerStatsHandler();
			statsHandler.configureLogging(logfile);
			boolean scrapeAll = false;
			if(inputRound != 0) {
				scrapeAll = true;
			}
			statsHandler.execute(round, scrapeAll);
		}
	}

	private void calcuateScores(int round) {
		loggerUtils.log("info", "Calculating scores");
		ScoresCalculatorHandler scoresCalculator = new ScoresCalculatorHandler();
		scoresCalculator.configureLogging(logfile);
		scoresCalculator.execute(round);
	}

	private void calculateLadder(int round, boolean isFinal) {
		if(round <= 18) {
			loggerUtils.log("info", "Calculating Ladder");
			LadderCalculatorHandler ladderCalculator = new LadderCalculatorHandler();
			ladderCalculator.configureLogging(logfile);
			if(isFinal) {
				loggerUtils.log("info", "Ladder is not live");
				ladderCalculator.execute(round, false);
			} else {
				loggerUtils.log("info", "Ladder is live");
				ladderCalculator.execute(round, true);
			}
		}
	}

	private void sendReport(int round, boolean isFinal, boolean sendReport) {
		if(sendReport) {
			loggerUtils.log("info", "Writing report");
			ResultsReport resultsReport = new ResultsReport();
			resultsReport.configureLogging(logfile);
			resultsReport.execute(round, isFinal, emailOverride);
		}
	}
	
	public static void main(String[] args) {
		
		Options options = new Options();
		
		Option roundOpt  = Option.builder("r").argName("round").hasArg().desc("round to run on").type(Number.class).required().build();
		Option emailOPt = Option.builder("e").argName("email").hasArg().desc("override email distribution").build();
		Option finalOpt = new Option("f", "final run");
		Option skipStatsOpt = new Option("ss", "skip stats download");
		Option sendReportOpt = new Option("rp", "send report");
		
		options.addOption(roundOpt);
		options.addOption(emailOPt);
		options.addOption(finalOpt);
		options.addOption(skipStatsOpt);
		options.addOption(sendReportOpt);
		
		try {
			String email = null;
			int round = 0;
			boolean isFinal = false;
			boolean skipStats = false;
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
			if(cli.hasOption("rp")) {
				sendReport = true;
			}
			
			ResultsHandler resultsHandler = new ResultsHandler();
			resultsHandler.configureLogging("ResultsHandler_R" + round);
			resultsHandler.execute(round, isFinal, email, skipStats, sendReport);
			
			System.exit(0);

		} catch (ParseException ex) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "RawStatsReport", options );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
