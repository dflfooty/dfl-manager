package net.dflmngr.reports;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import net.dflmngr.handlers.LadderCalculatorHandler;
//import net.dflmngr.jndi.JndiProvider;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflFixture;
import net.dflmngr.model.entity.DflLadder;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflPlayerPredictedScores;
import net.dflmngr.model.entity.DflPlayerScores;
//import net.dflmngr.model.entity.DflRoundInfo;
//import net.dflmngr.model.entity.DflRoundMapping;
import net.dflmngr.model.entity.DflSelectedPlayer;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.entity.DflTeamPredictedScores;
import net.dflmngr.model.entity.DflTeamScores;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.entity.RawPlayerStats;
import net.dflmngr.model.service.AflFixtureService;
import net.dflmngr.model.service.DflFixtureService;
import net.dflmngr.model.service.DflLadderService;
import net.dflmngr.model.service.DflPlayerPredictedScoresService;
import net.dflmngr.model.service.DflPlayerScoresService;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.DflRoundInfoService;
import net.dflmngr.model.service.DflSelectedTeamService;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.DflTeamPredictedScoresService;
import net.dflmngr.model.service.DflTeamScoresService;
import net.dflmngr.model.service.DflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.InsAndOutsService;
import net.dflmngr.model.service.RawPlayerStatsService;
import net.dflmngr.model.service.impl.AflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflFixtureServiceImpl;
import net.dflmngr.model.service.impl.DflLadderServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerPredictedScoresServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerScoresServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflRoundInfoServiceImpl;
import net.dflmngr.model.service.impl.DflSelectedTeamServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamPredictedScoresServiceImpl;
import net.dflmngr.model.service.impl.DflTeamScoresServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.model.service.impl.InsAndOutsServiceImpl;
import net.dflmngr.model.service.impl.RawPlayerStatsServiceImpl;
import net.dflmngr.reports.struct.ResultsFixtureTabTeamStruct;
//import net.dflmngr.utils.AmazonS3Utils;
import net.dflmngr.utils.DflmngrUtils;
import net.dflmngr.utils.EmailUtils;

public class ResultsReport {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "RoundProgress";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	RawPlayerStatsService rawPlayerStatsService;
	DflPlayerScoresService dflPlayerScoresService;
	DflTeamScoresService dflTeamScoresService;
	DflFixtureService dflFixtureService;
	DflSelectedTeamService dflSelectedTeamService;
	GlobalsService globalsService;
	DflTeamService dflTeamService;
	DflPlayerService dflPlayerService;
	AflFixtureService aflFixtureService;
	DflLadderService dflLadderService;
	DflPlayerPredictedScoresService dflPlayerPredictedScoresService;
	DflTeamPredictedScoresService dflTeamPredictedScoresService;
	DflRoundInfoService dflRoundInfoService;
	InsAndOutsService insAndOutsService;
	DflTeamPlayerService dflTeamPlayerService;
	
	String emailOverride;
	
	String[] resultsSpreadsheetHeaders = {"Player", "D", "M", "HO", "FF", "FA", "T", "G"};
	String[] fixtureSheetHeader = {"No.", "Player", "Pos", "K", "H", "D", "M", "HO", "FF", "FA", "T", "G", "B", "Score", "Predicted", "Trend"};
	String[] ladderHeader = {"Team", "W", "L", "D", "For", "Ave", "Agst", "Av", "Pts", "%"};
	String[] liveLadderHeader = {"Team", "Pts", "%"};
	
	Map<String, Integer> playersPlayedCount;
	Map<String, Integer> selectedPlayersCount;
	Map<String, Integer> currentPredictedTeamScores;
	
	Map<String, RawPlayerStats> playerStats;
	Map<Integer, DflPlayerScores> playerScores;
	Map<Integer, DflPlayerPredictedScores> playerPredictedScores;
	Map<String, DflTeamScores> teamScores;
	Map<String, DflTeamPredictedScores> teamPredictedScores;
	List<DflFixture> roundFixtures;
	
	public ResultsReport() {
		rawPlayerStatsService = new RawPlayerStatsServiceImpl();
		dflPlayerScoresService = new DflPlayerScoresServiceImpl();
		dflTeamScoresService = new DflTeamScoresServiceImpl();
		dflFixtureService = new DflFixtureServiceImpl();
		dflSelectedTeamService = new DflSelectedTeamServiceImpl();
		globalsService = new GlobalsServiceImpl();
		dflTeamService = new DflTeamServiceImpl();
		dflPlayerService = new DflPlayerServiceImpl();
		aflFixtureService = new AflFixtureServiceImpl();
		dflLadderService = new DflLadderServiceImpl();
		dflPlayerPredictedScoresService = new DflPlayerPredictedScoresServiceImpl();
		dflTeamPredictedScoresService = new DflTeamPredictedScoresServiceImpl();
		dflRoundInfoService = new DflRoundInfoServiceImpl();
		insAndOutsService = new InsAndOutsServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();
		
		playersPlayedCount = new HashMap<>();
		selectedPlayersCount = new HashMap<>();
		currentPredictedTeamScores = new HashMap<>();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round, boolean isFinal, String emailOverride) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "Executing ResultsReport for rounds: {}, is final: {}", round, isFinal);
			
			if(emailOverride != null && !emailOverride.equals("")) {
				loggerUtils.log("info", "Overriding email with: {}", emailOverride);
				this.emailOverride = emailOverride;
			}
			
			playerStats = rawPlayerStatsService.getForRoundWithKey(round);
			playerScores = dflPlayerScoresService.getForRoundWithKey(round);
			playerPredictedScores = dflPlayerPredictedScoresService.getForRoundWithKey(round);
			teamScores = dflTeamScoresService.getForRoundWithKey(round);
			teamPredictedScores = dflTeamPredictedScoresService.getForRoundWithKey(round);
			roundFixtures = dflFixtureService.getFixturesForRound(round);
			
			String reportName = writeReport(round, isFinal);
			
			loggerUtils.log("info", "Sending Report");
			emailReport(reportName, round, isFinal);
			
			rawPlayerStatsService.close();
			dflPlayerScoresService.close();
			dflTeamScoresService.close();
			dflFixtureService.close();
			dflSelectedTeamService.close();
			globalsService.close();
			dflTeamService.close();
			dflPlayerService.close();
			aflFixtureService.close();
			dflPlayerPredictedScoresService.close();
			insAndOutsService.close();
			dflTeamPlayerService.close();
			
			loggerUtils.log("info", "ResultsReport Completed");
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
		
	}
	
	private String writeReport(int round, boolean isFinal) throws Exception {
		
		String reportName = "";
		if(isFinal) {
			reportName = "ResultsReport_Round_" + round + "_FINAL_" + DflmngrUtils.getNowStr() + ".xlsx";
		} else {
			reportName = "ResultsReport_Round_" + round + "_" + DflmngrUtils.getNowStr() + ".xlsx";
		}
		
		Path reportDir = Paths.get(globalsService.getAppDir(), globalsService.getReportDir(), "resultsReport");
		
		File directory = new File(reportDir.toString());
	    if (!directory.exists()){
	        directory.mkdirs();
	    }
		
		Path reportLocation = Paths.get(reportDir.toString(), reportName);
		
		loggerUtils.log("info", "Writing Results Report");
		loggerUtils.log("info", "Report name: {}", reportName);
		loggerUtils.log("info", "Report location: {}", reportLocation);
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		
		XSSFSheet sheet = workbook.createSheet("ResultsSpreadsheet");
		writeResultsSpreadsheetHeaders(workbook);
		writeResultsSpreadsheetStats(sheet);
		
		for(DflFixture fixture : roundFixtures) {
			sheet = workbook.createSheet(fixture.getHomeTeam() + " vs " + fixture.getAwayTeam());
			writeFixtureSheetHeaders(sheet, fixture);
			writeFixtureSheetData(sheet, fixture);
		}
		
		for(int i = 0; i < workbook.getNumberOfSheets(); i++) {
			sheet = workbook.getSheetAt(i);
			for(int j = 0; j < sheet.getRow(1).getPhysicalNumberOfCells(); j++) {
				sheet.autoSizeColumn(j);
			}
		}
		
		OutputStream out = Files.newOutputStream(reportLocation);
		workbook.write(out);
		workbook.close();
		out.close();
		
		//String s3key = Paths.get("resultsReport", reportName).toString();
		
		//AmazonS3Utils.uploadToS3(s3key, reportLocation.toString());
		
		return reportLocation.toString();
	}
	
	private void writeResultsSpreadsheetHeaders(XSSFWorkbook workbook) {
		
		loggerUtils.log("info", "Writing ResultsSpreadsheet Header Rows");
		
		XSSFSheet sheet = workbook.getSheet("ResultsSpreadsheet");
		XSSFRow row = sheet.createRow(0);
		
		XSSFCellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		//font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
		font.setBold(true);
		style.setFont(font);
				
		for(int i = 0; i < this.resultsSpreadsheetHeaders.length; i++) {
			XSSFCell cell = row.createCell(i);
			cell.setCellValue(this.resultsSpreadsheetHeaders[i]);
			cell.setCellStyle(style);
		}
	}
	
	private void writeResultsSpreadsheetStats(XSSFSheet sheet) {
		
		loggerUtils.log("info", "Writing ResultsSpreadsheet report data");
		
		for(RawPlayerStats stats : playerStats.values()) {
			XSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
			
			for(int i = 0; i < this.resultsSpreadsheetHeaders.length; i++) {
				XSSFCell cell = row.createCell(i);
				
				switch(i) {
					case 0: cell.setCellValue(stats.getName()); break;
					case 1: cell.setCellValue(stats.getDisposals()); break;
					case 2: cell.setCellValue(stats.getMarks()); break;
					case 3: cell.setCellValue(stats.getHitouts()); break;
					case 4: cell.setCellValue(stats.getFreesFor()); break;
					case 5: cell.setCellValue(stats.getFreesAgainst()); break;
					case 6: cell.setCellValue(stats.getTackles()); break;
					case 7: cell.setCellValue(stats.getGoals()); break;
				}
			}
		}
	}
	
	private void writeFixtureSheetHeaders(XSSFSheet sheet, DflFixture fixture) {
		
		loggerUtils.log("info", "Writing fixture sheet Header Rows");
		
		XSSFRow row = sheet.createRow(0);
		
		XSSFCellStyle style = sheet.getWorkbook().createCellStyle();
		XSSFFont font = sheet.getWorkbook().createFont();
		//font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
		font.setBold(true);
		style.setFont(font);
		
		XSSFCellStyle teamNameStyle = sheet.getWorkbook().createCellStyle();
		teamNameStyle.setFont(font);
		//teamNameStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		teamNameStyle.setAlignment(HorizontalAlignment.CENTER);
		
		DflTeam team = dflTeamService.get(fixture.getHomeTeam());
		XSSFCell cell = row.createCell(0);
		cell.setCellValue(team.getName());
		cell.setCellStyle(teamNameStyle);
		
		team = dflTeamService.get(fixture.getAwayTeam());
		cell = row.createCell(fixtureSheetHeader.length + 1);
		cell.setCellValue(team.getName());
		cell.setCellStyle(teamNameStyle);
		
		sheet.addMergedRegion(new CellRangeAddress(0,0,0,(fixtureSheetHeader.length - 1)));
		sheet.addMergedRegion(new CellRangeAddress(0,0,(fixtureSheetHeader.length + 1),(fixtureSheetHeader.length * 2)));
		
		row = sheet.createRow(1);
		
		for(int i = 0; i < fixtureSheetHeader.length; i++) {
			cell = row.createCell(i);
			cell.setCellValue(fixtureSheetHeader[i]);
			cell.setCellStyle(style);
		}
		
		cell = row.createCell(fixtureSheetHeader.length);
		cell.setCellValue("");
		
		for(int i = 0; i < fixtureSheetHeader.length; i++) {
			cell = row.createCell(fixtureSheetHeader.length + 1 + i);
			cell.setCellValue(fixtureSheetHeader[i]);
			cell.setCellStyle(style);
		}
	}
	
	private void writeFixtureSheetData(XSSFSheet sheet, DflFixture fixture) throws Exception {
		
		loggerUtils.log("info", "Writing fixture sheet data");
		
		XSSFCellStyle style = sheet.getWorkbook().createCellStyle();
		XSSFFont font = sheet.getWorkbook().createFont();
		//font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
		font.setBold(true);
		style.setFont(font);
		
		List<ResultsFixtureTabTeamStruct> homeTeamData = new ArrayList<>();
		List<ResultsFixtureTabTeamStruct> awayTeamData = new ArrayList<>();
		List<ResultsFixtureTabTeamStruct> homeTeamEmgData = new ArrayList<>();
		List<ResultsFixtureTabTeamStruct> awayTeamEmgData = new ArrayList<>();
		
		List<DflSelectedPlayer> selectedHomeTeam = dflSelectedTeamService.getSelectedTeamForRound(fixture.getRound(), fixture.getHomeTeam());
		List<DflSelectedPlayer> selectedAwayTeam = dflSelectedTeamService.getSelectedTeamForRound(fixture.getRound(), fixture.getAwayTeam());
		
		//DflRoundInfo dflRoundInfo = dflRoundInfoService.get(fixture.getRound());
		//List<String> playedTeams = new ArrayList<>();
		//int aflRound = 0;
		//for(DflRoundMapping roundMapping : dflRoundInfo.getRoundMapping()) {
		//	int currentAflRound = roundMapping.getAflRound();
		//	if(aflRound != currentAflRound) {
		//		playedTeams.addAll(aflFixtureService.getAflTeamsPlayedForRound(currentAflRound));
		//		aflRound = currentAflRound;
		//	}
		//}
		
		int playersPlayed = 0;
		int currentPredictedScore = 0;
		int homeTotalTrend = 0;
		
		boolean homeEmgStar = false;
		boolean homeEmgDoubleStar = false;
		
		for(DflSelectedPlayer selectedPlayer : selectedHomeTeam) {
			ResultsFixtureTabTeamStruct playerRec = new ResultsFixtureTabTeamStruct();
			
			DflPlayerScores score = playerScores.get(selectedPlayer.getPlayerId());
			DflPlayerPredictedScores predictedScore = playerPredictedScores.get(selectedPlayer.getPlayerId());
			DflPlayer player = dflPlayerService.get(selectedPlayer.getPlayerId());
			
			String playerName = player.getFirstName() + " " + player.getLastName();
			if(selectedPlayer.getReplacementInd() != null && !selectedPlayer.getReplacementInd().isEmpty()) {
				playerName = playerName + selectedPlayer.getReplacementInd();
			}
						
			playerRec.setNo(selectedPlayer.getTeamPlayerId());
			playerRec.setPlayer(playerName);
			playerRec.setPosition(player.getPosition());
			
			if(predictedScore == null) {
				playerRec.setPredicted(25);
			} else {
				playerRec.setPredicted(predictedScore.getPredictedScore());
			}
		

			if(selectedPlayer.hasPlayed()) {
				if(selectedPlayer.isDnp()) {
					playerRec.setPlayed(true);
					playerRec.setDnp(true);
					playerRec.setKicks("0");
					playerRec.setHandballs("0");
					playerRec.setDisposals("0");
					playerRec.setMarks("0");
					playerRec.setHitouts("0");
					playerRec.setFreesFor("0");
					playerRec.setFreesAgainst("0");
					playerRec.setTackles("0");
					playerRec.setGoals("0");
					playerRec.setBehinds("0");
					playerRec.setScore("dnp");
					playerRec.setTrend("-" + playerRec.getPredicted());
				} else {
					RawPlayerStats stats =  playerStats.get(score.getAflPlayerId());
					
					playerRec.setPlayed(true);
					playerRec.setDnp(false);
					playerRec.setKicksInt(stats.getKicks());
					playerRec.setHandballsInt(stats.getHandballs());
					playerRec.setDisposalsInt(stats.getDisposals());
					playerRec.setMarksInt(stats.getMarks());
					playerRec.setHitoutsInt(stats.getHitouts());
					playerRec.setFreesForInt(stats.getFreesFor());
					playerRec.setFreesAgainstInt(stats.getFreesAgainst());
					playerRec.setTacklesInt(stats.getTackles());
					playerRec.setGoalsInt(stats.getGoals());
					playerRec.setBehindsInt(stats.getBehinds());
					playerRec.setScoreInt(score.getScore());
					playerRec.setTrendInt(playerRec.getScoreInt() - playerRec.getPredicted());
				}
			} else {
				playerRec.setPlayed(false);
				playerRec.setDnp(false);
				playerRec.setKicks("");
				playerRec.setHandballs("");
				playerRec.setDisposals("");
				playerRec.setMarks("");
				playerRec.setHitouts("");
				playerRec.setFreesFor("");
				playerRec.setFreesAgainst("");
				playerRec.setTackles("");
				playerRec.setGoals("");
				playerRec.setBehinds("");
				playerRec.setScore("");
				playerRec.setTrend("");
			}
			
			/*
			if(score == null) {
				if(playedTeams.contains(DflmngrUtils.dflAflTeamMap.get(player.getAflClub()))) {
					playerRec.setPlayed(true);
					playerRec.setDnp(true);
					playerRec.setKicks("0");
					playerRec.setHandballs("0");
					playerRec.setDisposals("0");
					playerRec.setMarks("0");
					playerRec.setHitouts("0");
					playerRec.setFreesFor("0");
					playerRec.setFreesAgainst("0");
					playerRec.setTackles("0");
					playerRec.setGoals("0");
					playerRec.setBehinds("0");
					playerRec.setScore("dnp");
					playerRec.setTrend("-" + playerRec.getPredicted());
				} else {
					playerRec.setPlayed(false);
					playerRec.setDnp(false);
					playerRec.setKicks("");
					playerRec.setHandballs("");
					playerRec.setDisposals("");
					playerRec.setMarks("");
					playerRec.setHitouts("");
					playerRec.setFreesFor("");
					playerRec.setFreesAgainst("");
					playerRec.setTackles("");
					playerRec.setGoals("");
					playerRec.setBehinds("");
					playerRec.setScore("");
					playerRec.setTrend("");
				}				
			} else {
				RawPlayerStats stats =  playerStats.get(score.getAflPlayerId());
				
				playerRec.setPlayed(true);
				playerRec.setDnp(false);
				playerRec.setKicksInt(stats.getKicks());
				playerRec.setHandballsInt(stats.getHandballs());
				playerRec.setDisposalsInt(stats.getDisposals());
				playerRec.setMarksInt(stats.getMarks());
				playerRec.setHitoutsInt(stats.getHitouts());
				playerRec.setFreesForInt(stats.getFreesFor());
				playerRec.setFreesAgainstInt(stats.getFreesAgainst());
				playerRec.setTacklesInt(stats.getTackles());
				playerRec.setGoalsInt(stats.getGoals());
				playerRec.setBehindsInt(stats.getBehinds());
				playerRec.setScoreInt(score.getScore());
				playerRec.setTrendInt(playerRec.getScoreInt() - playerRec.getPredicted());
			}
			*/
		
			if(selectedPlayer.isScoreUsed()) {
				homeTeamData.add(playerRec);
				if(playerRec.hasPlayed()) {
					playersPlayed++;
					if(!playerRec.isDnp()) {
						currentPredictedScore = currentPredictedScore + playerRec.getScoreInt();
					}
					homeTotalTrend = homeTotalTrend + playerRec.getTrendInt();
				} else {
					currentPredictedScore = currentPredictedScore + playerRec.getPredicted();
				}
				if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("*")) {
					homeEmgStar = true;
				} else if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("**")) {
					homeEmgDoubleStar = true;
				}
			} else {
				if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("*")) {
					playerRec.setEmgSort(1);
				} else if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("**")) {
					playerRec.setEmgSort(2);
				} else {
					playerRec.setEmgSort(selectedPlayer.isEmergency());
				}
				homeTeamEmgData.add(playerRec);
			}
			
			/*
			if(selectedPlayer.isEmergency() == 0) {
				homeTeamData.add(playerRec);
			} else {
				playerRec.setEmgSort(selectedPlayer.isEmergency());
				homeTeamEmgData.add(playerRec);
				if(selectedPlayer.isScoreUsed()) {
					homeEmgUsed = true;
				}
			}
			*/
		}
		
		playersPlayedCount.put(fixture.getHomeTeam(), playersPlayed);
		//selectedPlayersCount.put(fixture.getHomeTeam(), selectedHomeTeam.size());
		selectedPlayersCount.put(fixture.getHomeTeam(), homeTeamData.size());
		currentPredictedTeamScores.put(fixture.getHomeTeam(), currentPredictedScore);
		playersPlayed = 0;
		currentPredictedScore = 0;
		int awayTotalTrend = 0;
		
		boolean awayEmgStar = false;
		boolean awayEmgDoubleStar = false;
		
		for(DflSelectedPlayer selectedPlayer : selectedAwayTeam) {
			ResultsFixtureTabTeamStruct playerRec = new ResultsFixtureTabTeamStruct();
			
			DflPlayerScores score = playerScores.get(selectedPlayer.getPlayerId());
			DflPlayerPredictedScores predictedScore = playerPredictedScores.get(selectedPlayer.getPlayerId());
			DflPlayer player = dflPlayerService.get(selectedPlayer.getPlayerId());
			
			String playerName = player.getFirstName() + " " + player.getLastName();
			if(selectedPlayer.getReplacementInd() != null && !selectedPlayer.getReplacementInd().isEmpty()) {
				playerName = playerName + selectedPlayer.getReplacementInd();
			}
			
			playerRec.setNo(selectedPlayer.getTeamPlayerId());
			playerRec.setPlayer(playerName);
			playerRec.setPosition(player.getPosition());
			
			if(predictedScore == null) {
				playerRec.setPredicted(25);
			} else {
				playerRec.setPredicted(predictedScore.getPredictedScore());
			}
			
			if(selectedPlayer.hasPlayed()) {
				if(selectedPlayer.isDnp()) {
					playerRec.setPlayed(true);
					playerRec.setDnp(true);
					playerRec.setKicks("0");
					playerRec.setHandballs("0");
					playerRec.setDisposals("0");
					playerRec.setMarks("0");
					playerRec.setHitouts("0");
					playerRec.setFreesFor("0");
					playerRec.setFreesAgainst("0");
					playerRec.setTackles("0");
					playerRec.setGoals("0");
					playerRec.setBehinds("0");
					playerRec.setScore("dnp");
					playerRec.setTrend("-" + playerRec.getPredicted());
				} else {
					RawPlayerStats stats =  playerStats.get(score.getAflPlayerId());
					
					playerRec.setPlayed(true);
					playerRec.setDnp(false);
					playerRec.setKicksInt(stats.getKicks());
					playerRec.setHandballsInt(stats.getHandballs());
					playerRec.setDisposalsInt(stats.getDisposals());
					playerRec.setMarksInt(stats.getMarks());
					playerRec.setHitoutsInt(stats.getHitouts());
					playerRec.setFreesForInt(stats.getFreesFor());
					playerRec.setFreesAgainstInt(stats.getFreesAgainst());
					playerRec.setTacklesInt(stats.getTackles());
					playerRec.setGoalsInt(stats.getGoals());
					playerRec.setBehindsInt(stats.getBehinds());
					playerRec.setScoreInt(score.getScore());
					playerRec.setTrendInt(playerRec.getScoreInt() - playerRec.getPredicted());
				}
			} else {
				playerRec.setPlayed(false);
				playerRec.setDnp(false);
				playerRec.setKicks("");
				playerRec.setHandballs("");
				playerRec.setDisposals("");
				playerRec.setMarks("");
				playerRec.setHitouts("");
				playerRec.setFreesFor("");
				playerRec.setFreesAgainst("");
				playerRec.setTackles("");
				playerRec.setGoals("");
				playerRec.setBehinds("");
				playerRec.setScore("");
				playerRec.setTrend("");
			}
			
			/*
			if(score == null) {
				if(playedTeams.contains(DflmngrUtils.dflAflTeamMap.get(player.getAflClub()))) {
					playerRec.setPlayed(true);
					playerRec.setDnp(true);
					playerRec.setKicks("0");
					playerRec.setHandballs("0");
					playerRec.setDisposals("0");
					playerRec.setMarks("0");
					playerRec.setHitouts("0");
					playerRec.setFreesFor("0");
					playerRec.setFreesAgainst("0");
					playerRec.setTackles("0");
					playerRec.setGoals("0");
					playerRec.setBehinds("0");
					playerRec.setScore("dnp");
					playerRec.setTrend("-" + playerRec.getPredicted());
				} else {
					playerRec.setPlayed(false);
					playerRec.setDnp(false);
					playerRec.setKicks("");
					playerRec.setHandballs("");
					playerRec.setDisposals("");
					playerRec.setMarks("");
					playerRec.setHitouts("");
					playerRec.setFreesFor("");
					playerRec.setFreesAgainst("");
					playerRec.setTackles("");
					playerRec.setGoals("");
					playerRec.setBehinds("");
					playerRec.setScore("");
					playerRec.setTrend("");
				}				
			} else {
				RawPlayerStats stats =  playerStats.get(score.getAflPlayerId());
				
				playerRec.setPlayed(true);
				playerRec.setDnp(false);
				playerRec.setKicksInt(stats.getKicks());
				playerRec.setHandballsInt(stats.getHandballs());
				playerRec.setDisposalsInt(stats.getDisposals());
				playerRec.setMarksInt(stats.getMarks());
				playerRec.setHitoutsInt(stats.getHitouts());
				playerRec.setFreesForInt(stats.getFreesFor());
				playerRec.setFreesAgainstInt(stats.getFreesAgainst());
				playerRec.setTacklesInt(stats.getTackles());
				playerRec.setGoalsInt(stats.getGoals());
				playerRec.setBehindsInt(stats.getBehinds());
				playerRec.setScoreInt(score.getScore());
				playerRec.setTrendInt(playerRec.getScoreInt() - playerRec.getPredicted());
			}
			/*/
			
			if(selectedPlayer.isScoreUsed()) {
				awayTeamData.add(playerRec);
				if(playerRec.hasPlayed()) {
					playersPlayed++;
					if(!playerRec.isDnp()) {
						currentPredictedScore = currentPredictedScore + playerRec.getScoreInt();
					}
					awayTotalTrend = awayTotalTrend + playerRec.getTrendInt();
				} else {
					currentPredictedScore = currentPredictedScore + playerRec.getPredicted();
				}
				if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("*")) {
					awayEmgStar = true;
				} else if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("**")) {
					awayEmgDoubleStar = true;
				}
			} else {
				if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("*")) {
					playerRec.setEmgSort(1);
				} else if(selectedPlayer.getReplacementInd() != null && selectedPlayer.getReplacementInd().equals("**")) {
					playerRec.setEmgSort(2);
				} else {
					playerRec.setEmgSort(selectedPlayer.isEmergency());
				}
				awayTeamEmgData.add(playerRec);
			}
			
			/*
			if(selectedPlayer.isEmergency() == 0) {
				awayTeamData.add(playerRec);
			} else {
				playerRec.setEmgSort(selectedPlayer.isEmergency());
				awayTeamEmgData.add(playerRec);
				if(selectedPlayer.isScoreUsed()) {
					awayEmgUsed = true;
				}
			}
			*/
		}
		
		playersPlayedCount.put(fixture.getAwayTeam(), playersPlayed);
		//selectedPlayersCount.put(fixture.getAwayTeam(), selectedAwayTeam.size());
		selectedPlayersCount.put(fixture.getAwayTeam(), awayTeamData.size());
		currentPredictedTeamScores.put(fixture.getAwayTeam(), currentPredictedScore);
		
		Collections.sort(homeTeamData);
		Collections.sort(awayTeamData);
		
		Comparator<ResultsFixtureTabTeamStruct> comparator = Comparator.comparingInt(ResultsFixtureTabTeamStruct::getEmgSort);
		homeTeamEmgData.sort(comparator);
		awayTeamEmgData.sort(comparator);
		
		writeStatsData(sheet, homeTeamData, awayTeamData);
		
		int totalsCellBase = fixtureSheetHeader.length - 4;
		
		XSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
		XSSFCell cell = row.createCell(totalsCellBase);
		cell.setCellValue("Total");
		cell.setCellStyle(style);
		
		cell = row.createCell(totalsCellBase + 1);
		
		/*
		if(homeEmgUsed) {
			cell.setCellValue(teamScores.get(fixture.getHomeTeam()).getScore() + "*");
		} else {
			cell.setCellValue(teamScores.get(fixture.getHomeTeam()).getScore());
		}
		*/
		cell.setCellValue(teamScores.get(fixture.getHomeTeam()).getScore());
		cell.setCellStyle(style);
		
		if(playersPlayedCount.get(fixture.getHomeTeam()) == selectedPlayersCount.get(fixture.getHomeTeam())) {
			int teamPredictedScore = teamPredictedScores.get(fixture.getHomeTeam()).getPredictedScore();
			cell = row.createCell(totalsCellBase + 2);
			cell.setCellValue(teamPredictedScore);
			cell.setCellStyle(style);
		} else {
			cell = row.createCell(totalsCellBase + 2);
			cell.setCellValue(currentPredictedTeamScores.get(fixture.getHomeTeam()));
			cell.setCellStyle(style);
		}
		
		cell = row.createCell(totalsCellBase + 3);
		cell.setCellValue(homeTotalTrend);
		cell.setCellStyle(style);
		
		totalsCellBase = totalsCellBase + fixtureSheetHeader.length + 1;
		
		cell = row.createCell(totalsCellBase);
		cell.setCellValue("Total");
		cell.setCellStyle(style);
		
		cell = row.createCell(totalsCellBase + 1);
		
		/*
		if(awayEmgUsed) {
			cell.setCellValue(teamScores.get(fixture.getAwayTeam()).getScore() + "*");
		} else {
			cell.setCellValue(teamScores.get(fixture.getAwayTeam()).getScore());
		}
		*/
		cell.setCellValue(teamScores.get(fixture.getAwayTeam()).getScore());
		cell.setCellStyle(style);
		
		if(playersPlayedCount.get(fixture.getAwayTeam()) == selectedPlayersCount.get(fixture.getAwayTeam())) {
			int teamPredictedScore = teamPredictedScores.get(fixture.getAwayTeam()).getPredictedScore();
			cell = row.createCell(totalsCellBase + 2);
			cell.setCellValue(teamPredictedScore);
			cell.setCellStyle(style);
		} else {
			cell = row.createCell(totalsCellBase + 2);
			cell.setCellValue(currentPredictedTeamScores.get(fixture.getAwayTeam()));
			cell.setCellStyle(style);
		};
		
		cell = row.createCell(totalsCellBase + 3);
		cell.setCellValue(awayTotalTrend);
		cell.setCellStyle(style);
		
		/*
		if(!homeEmgUsed && !awayEmgUsed) {
			sheet.createRow(sheet.getLastRowNum()+1);
		} else {
			row = sheet.createRow(sheet.getLastRowNum()+1);
		}
		*/
		sheet.createRow(sheet.getLastRowNum()+1);
		
		/*
		if(homeEmgUsed) {
			cell = row.createCell(0);
			cell.setCellValue("* Score includes one or more emerencies");
			sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),0,(fixtureSheetHeader.length - 1)));
		}
		if(awayEmgUsed) {
			cell = row.createCell(fixtureSheetHeader.length + 1);
			cell.setCellValue("* Score includes one or more emerencies");
			sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),(fixtureSheetHeader.length + 1),(fixtureSheetHeader.length * 2)));
		}
		*/

		row = sheet.createRow(sheet.getLastRowNum()+1);
		
		style = sheet.getWorkbook().createCellStyle();
		font = sheet.getWorkbook().createFont();
		font.setBold(true);
		style.setFont(font);
		
		XSSFCellStyle emgStyle = sheet.getWorkbook().createCellStyle();
		emgStyle.setFont(font);
		emgStyle.setAlignment(HorizontalAlignment.CENTER);
		
		cell = row.createCell(0);
		cell.setCellValue("Emergencies");
		cell.setCellStyle(emgStyle);
		
		cell = row.createCell(fixtureSheetHeader.length + 1);
		cell.setCellValue("Emergencies");
		cell.setCellStyle(emgStyle);
		
		sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),0,(fixtureSheetHeader.length - 1)));
		sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),(fixtureSheetHeader.length + 1),(fixtureSheetHeader.length * 2)));
		
		row = sheet.createRow(sheet.getLastRowNum()+1);;
		
		for(int i = 0; i < fixtureSheetHeader.length; i++) {
			cell = row.createCell(i);
			cell.setCellValue(fixtureSheetHeader[i]);
			cell.setCellStyle(style);
		}
		
		cell = row.createCell(fixtureSheetHeader.length);
		cell.setCellValue("");
		
		for(int i = 0; i < fixtureSheetHeader.length; i++) {
			cell = row.createCell(fixtureSheetHeader.length + 1 + i);
			cell.setCellValue(fixtureSheetHeader[i]);
			cell.setCellStyle(style);
		}
		
		writeStatsData(sheet, homeTeamEmgData, awayTeamEmgData);
		
		if(homeEmgStar || homeEmgDoubleStar || awayEmgStar || awayEmgDoubleStar) {
			row = sheet.createRow(sheet.getLastRowNum()+1);
		
			if(homeEmgStar && !homeEmgDoubleStar) {
				cell = row.createCell(0);
				cell.setCellValue("* Replaced in selected 22 by an emergency");
				sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),0,(fixtureSheetHeader.length - 1)));
			} else if(!homeEmgStar && homeEmgDoubleStar) {
				cell = row.createCell(0);
				cell.setCellValue("** Replaced in selected 22 by an emergency");
				sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),0,(fixtureSheetHeader.length - 1)));
			} else if(homeEmgStar && homeEmgDoubleStar) {
				cell = row.createCell(0);
				cell.setCellValue("*/** Replaced in selected 22 by an emergency");
				sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),0,(fixtureSheetHeader.length - 1)));
			}
			
			if(awayEmgStar && !awayEmgDoubleStar) {
				cell = row.createCell(fixtureSheetHeader.length + 1);
				cell.setCellValue("* Replaced in selected 22 by an emergency");
				sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),(fixtureSheetHeader.length + 1),(fixtureSheetHeader.length * 2)));
			} else if(!awayEmgStar && awayEmgDoubleStar) {
				cell = row.createCell(fixtureSheetHeader.length + 1);
				cell.setCellValue("** Replaced in selected 22 by an emergency");
				sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),(fixtureSheetHeader.length + 1),(fixtureSheetHeader.length * 2)));
			} else if(awayEmgStar && awayEmgDoubleStar) {
				cell = row.createCell(fixtureSheetHeader.length + 1);
				cell.setCellValue("*/** Replaced in selected 22 by an emergency");
				sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),sheet.getLastRowNum(),(fixtureSheetHeader.length + 1),(fixtureSheetHeader.length * 2)));
			}
		}
	}
		
	private void writeStatsData(XSSFSheet sheet, List<ResultsFixtureTabTeamStruct> homeTeamData, List<ResultsFixtureTabTeamStruct> awayTeamData) {
		
		int currentStatsRow = sheet.getLastRowNum()+1;
		
		for(ResultsFixtureTabTeamStruct rec : homeTeamData) {
			
			XSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
			
			for(int i = 0; i < this.fixtureSheetHeader.length; i++) {
				XSSFCell cell = row.createCell(i);
				
				if(rec.hasPlayed()) {
					switch(i) {
						case 0: cell.setCellValue(rec.getNo()); break;
						case 1: cell.setCellValue(rec.getPlayer()); break;
						case 2: cell.setCellValue(rec.getPosition()); break;
						case 3: cell.setCellValue(rec.getKicksInt()); break;
						case 4: cell.setCellValue(rec.getHandballsInt()); break;
						case 5: cell.setCellValue(rec.getDisposalsInt()); break;
						case 6: cell.setCellValue(rec.getMarksInt()); break;
						case 7: cell.setCellValue(rec.getHitoutsInt()); break;
						case 8: cell.setCellValue(rec.getFreesForInt()); break;
						case 9: cell.setCellValue(rec.getFreesAgainstInt()); break;
						case 10: cell.setCellValue(rec.getTacklesInt()); break;
						case 11: cell.setCellValue(rec.getGoalsInt()); break;
						case 12: cell.setCellValue(rec.getBehindsInt()); break;
						case 13: if(rec.isDnp()) {
									cell.setCellValue(rec.getScore()); break;
								} else {
									cell.setCellValue(rec.getScoreInt()); break;
								}
						case 14: cell.setCellValue(rec.getPredicted()); break;
						case 15: cell.setCellValue(rec.getTrendInt()); break;
					}
				} else {
					switch(i) {
						case 0: cell.setCellValue(rec.getNo()); break;
						case 1: cell.setCellValue(rec.getPlayer()); break;
						case 2: cell.setCellValue(rec.getPosition()); break;
						case 3: cell.setCellValue(rec.getKicks()); break;
						case 4: cell.setCellValue(rec.getHandballs()); break;
						case 5: cell.setCellValue(rec.getDisposals()); break;
						case 6: cell.setCellValue(rec.getMarks()); break;
						case 7: cell.setCellValue(rec.getHitouts()); break;
						case 8: cell.setCellValue(rec.getFreesFor()); break;
						case 9: cell.setCellValue(rec.getFreesAgainst()); break;
						case 10: cell.setCellValue(rec.getTackles()); break;
						case 11: cell.setCellValue(rec.getGoals()); break;
						case 12: cell.setCellValue(rec.getBehinds()); break;
						case 13: cell.setCellValue(rec.getScore()); break;
						case 14: cell.setCellValue(rec.getPredicted()); break;
						case 15: cell.setCellValue(rec.getTrend()); break;
					}
				}
			}
		}
		
		for(ResultsFixtureTabTeamStruct rec : awayTeamData) {
			XSSFRow row = sheet.getRow(currentStatsRow);
			if(row == null) {
				row = sheet.createRow(currentStatsRow);
			}
			
			for(int i = 0; i < this.fixtureSheetHeader.length; i++) {
				XSSFCell cell = row.createCell(i + fixtureSheetHeader.length + 1);
				
				if(rec.hasPlayed()) {
					switch(i) {
						case 0: cell.setCellValue(rec.getNo()); break;
						case 1: cell.setCellValue(rec.getPlayer()); break;
						case 2: cell.setCellValue(rec.getPosition()); break;
						case 3: cell.setCellValue(rec.getKicksInt()); break;
						case 4: cell.setCellValue(rec.getHandballsInt()); break;
						case 5: cell.setCellValue(rec.getDisposalsInt()); break;
						case 6: cell.setCellValue(rec.getMarksInt()); break;
						case 7: cell.setCellValue(rec.getHitoutsInt()); break;
						case 8: cell.setCellValue(rec.getFreesForInt()); break;
						case 9: cell.setCellValue(rec.getFreesAgainstInt()); break;
						case 10: cell.setCellValue(rec.getTacklesInt()); break;
						case 11: cell.setCellValue(rec.getGoalsInt()); break;
						case 12: cell.setCellValue(rec.getBehindsInt()); break;
						case 13: if(rec.isDnp()) {
									cell.setCellValue(rec.getScore()); break;
								} else {
									cell.setCellValue(rec.getScoreInt()); break;
								}
						case 14: cell.setCellValue(rec.getPredicted()); break;
						case 15: cell.setCellValue(rec.getTrendInt()); break;
					}
				} else {
					switch(i) {
						case 0: cell.setCellValue(rec.getNo()); break;
						case 1: cell.setCellValue(rec.getPlayer()); break;
						case 2: cell.setCellValue(rec.getPosition()); break;
						case 3: cell.setCellValue(rec.getKicks()); break;
						case 4: cell.setCellValue(rec.getHandballs()); break;
						case 5: cell.setCellValue(rec.getDisposals()); break;
						case 6: cell.setCellValue(rec.getMarks()); break;
						case 7: cell.setCellValue(rec.getHitouts()); break;
						case 8: cell.setCellValue(rec.getFreesFor()); break;
						case 9: cell.setCellValue(rec.getFreesAgainst()); break;
						case 10: cell.setCellValue(rec.getTackles()); break;
						case 11: cell.setCellValue(rec.getGoals()); break;
						case 12: cell.setCellValue(rec.getBehinds()); break;
						case 13: cell.setCellValue(rec.getScore()); break;
						case 14: cell.setCellValue(rec.getPredicted()); break;
						case 15: cell.setCellValue(rec.getTrend()); break;
					}
				}
			}
			
			currentStatsRow++;
		}
	}
	
	private void emailReport(String reportName, int round, boolean isFinal) throws Exception {
		
		String dflMngrEmail = globalsService.getEmailConfig().get("dflmngrEmailAddr");
		
		String subject = "";
		String body = "";
		
		if(isFinal) {
			subject = "Results for DFL round " + round + " - FINAL";
			body = createEmailBodyFinal(round);
		} else {
			subject = "Stats for DFL round " + round + " - CURRENT";
			body = createEmailBody(round);
		}
		
		List<String> to = new ArrayList<>();

		if(emailOverride != null && !emailOverride.equals("")) {
			to.add(emailOverride);
		} else {
			List<DflTeam> teams = dflTeamService.findAll();
			for(DflTeam team : teams) {
				to.add(team.getCoachEmail());
			}
		}
		
		List<String> attachments = new ArrayList<>();
		attachments.add(reportName);
		
		loggerUtils.log("info", "Emailing to={}; reportName={}", to, reportName);
		EmailUtils.sendHtmlEmail(to, dflMngrEmail, subject, body, attachments);
	}
	
	private String createEmailBodyFinal(int round) {
		
		String body = "<html>";
		body = body + "<body>\n";
		body = "<p>Results for round " + round + ":</p>\n";
		body = body + "<p><ul type=none>\n";
				
		for(DflFixture fixture : roundFixtures) {
			DflTeam homeTeam = dflTeamService.get(fixture.getHomeTeam());
			int homeTeamScore = teamScores.get(fixture.getHomeTeam()).getScore();
			int homeTeamPredictedScore = teamPredictedScores.get(fixture.getHomeTeam()).getPredictedScore();
			
			DflTeam awayTeam = dflTeamService.get(fixture.getAwayTeam());
			int awayTeamScore = teamScores.get(fixture.getAwayTeam()).getScore();
			int awayTeamPredictedScore = teamPredictedScores.get(fixture.getAwayTeam()).getPredictedScore();
			
			String resultString = "";
			if(homeTeamScore > awayTeamScore) {
				resultString = " defeated ";
			} else {
				resultString = " defeated by ";
			}
			
			body = body + "<li>" + homeTeam.getName() + " " + homeTeamScore + " (" + homeTeamPredictedScore + ") " + 
				   resultString + awayTeam.getName() + " " + awayTeamScore + " (" + awayTeamPredictedScore + ") " + "</li>\n";
		}
		
		body = body + "</ul></p>\n";
		
		List<DflLadder> ladder = dflLadderService.getLadderForRound(round);
		Collections.sort(ladder, Collections.reverseOrder());
								
		body = body + "<p>Ladder:</p>\n";
		body = body + "<p><table border=1 style=\"border-collapse: collapse; border: 1px solid black;\">\n";
		body = body + "<tr>\n";
		body = body + "<th align=left style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">Team</th><th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">W</th>"
				    + "<th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">L</th><th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">D</th>"
				    + "<th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">For</th><th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">Av</th>"
				    + "<th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">Agst</th><th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">Av</th>"
				    + "<th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">Pts</th><th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%</th>";
		body = body + "</tr>\n";
		
		String tableFormat = "<td style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%s</td><td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%d</td>"
				           + "<td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%d</td><td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%d</td>"
				           + "<td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%d</td><td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%.2f</td>"
				           + "<td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%d</td><td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%.2f</td>"
				           + "<td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%d</td><td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%.2f</td>"
				           + "%n";
		
		for(DflLadder team : ladder) {
			String teamName = dflTeamService.get(team.getTeamCode()).getName();
			
			body = body + "<tr>\n";
			body = body + String.format(tableFormat, teamName, team.getWins(), team.getLosses(), team.getDraws(), team.getPointsFor(), team.getAverageFor(),
					          team.getPointsAgainst(), team.getAverageAgainst(), team.getPts(), team.getPercentage());
			body = body + "</tr>\n";
		}
		
		body = body + "</table></p>\n";
		body = body + "<p>Results attached.</p>\n";
		
		List<DflTeam> teams = dflTeamService.findAll();
		
		body = body + selectionSummary(round, teams);
		
		body = body + "<p>DFL Manager Admin</p>\n";
		body = body + "</div></body></html>";
		
		return body;
	}
	
	private String createEmailBody(int round) {
		
		//int awayTeamPredictedScore = 0;
		//int homeTeamPredictedScore = 0;
		
		Map<String, Integer> currentLadderScores = new HashMap<>();
		
		String body = "<html>";
		body = body + "<body>\n";
		body = "<p>Current scores for round " + round + ":</p>\n";
		body = body + "<p><ul type=none>\n";
		
		loggerUtils.log("info", "Team Scores:{}", teamScores);
		loggerUtils.log("info", "Predicted Scores:{}", teamPredictedScores);
		loggerUtils.log("info", "Current Predicted Scores:{}", currentPredictedTeamScores);
		
		loggerUtils.log("info", "Played count:{}", playersPlayedCount);
		loggerUtils.log("info", "Selected count:{}", selectedPlayersCount);
		
		
		for(DflFixture fixture : roundFixtures) {
			DflTeam homeTeam = dflTeamService.get(fixture.getHomeTeam());
			//int homeTeamScore = teamScores.get(fixture.getHomeTeam()).getScore();
			
			int homePlayersPlayed = playersPlayedCount.get(fixture.getHomeTeam());
			int homeSelectedSize = selectedPlayersCount.get(fixture.getHomeTeam());
			
			int homeTeamScore;
			int homeTeamPredictedScore;
			//if(playersPlayedCount.get(fixture.getHomeTeam()) == selectedPlayersCount.get(fixture.getHomeTeam())) {
			if(homePlayersPlayed == homeSelectedSize) {
				//homeTeamPredictedScore = homeTeamScore;
				homeTeamPredictedScore = teamScores.get(fixture.getHomeTeam()).getScore();
				homeTeamScore = teamPredictedScores.get(fixture.getHomeTeam()).getPredictedScore();
			} else {
				homeTeamPredictedScore = currentPredictedTeamScores.get(fixture.getHomeTeam());
				homeTeamScore = teamScores.get(fixture.getHomeTeam()).getScore();
			}
			//homeTeamPredictedScore = currentPredictedTeamScores.get(fixture.getHomeTeam());
			
			DflTeam awayTeam = dflTeamService.get(fixture.getAwayTeam());
			//int awayTeamScore = teamScores.get(fixture.getAwayTeam()).getScore();
					
			int awayPlayersPlayed = playersPlayedCount.get(fixture.getAwayTeam());
			int awaySelectedSize = selectedPlayersCount.get(fixture.getAwayTeam());
			
			int awayTeamScore;
			int awayTeamPredictedScore;
			//if(playersPlayedCount.get(fixture.getAwayTeam()) == selectedPlayersCount.get(fixture.getAwayTeam())) {
			if(awayPlayersPlayed == awaySelectedSize) {
				//awayTeamPredictedScore = awayTeamScore;
				awayTeamPredictedScore = teamScores.get(fixture.getAwayTeam()).getScore();
				awayTeamScore = teamPredictedScores.get(fixture.getAwayTeam()).getPredictedScore();
			} else {
				awayTeamPredictedScore = currentPredictedTeamScores.get(fixture.getAwayTeam());
				awayTeamScore = teamScores.get(fixture.getAwayTeam()).getScore();
			}
			//awayTeamPredictedScore = currentPredictedTeamScores.get(fixture.getAwayTeam());
			
			String resultString = "";
			//if(homeTeamScore > awayTeamScore) {
			if(homeTeamPredictedScore > awayTeamPredictedScore) {
				resultString = " leading ";
			} else {
				resultString = " lead by ";
			}
			
			/*
			body = body + "<li>" 
				   + homeTeam.getName() + " " + homeTeamScore + " (" + homeTeamPredictedScore + " - " + homePlayersPlayed + "/" + homeSelectedSize + " played)"
				   + resultString 
				   + awayTeam.getName() + " " + awayTeamScore + " (" + awayTeamPredictedScore + " - " + awayPlayersPlayed + "/" + awaySelectedSize + " played)"
				   + "</li>\n";
			*/
			body = body + "<li>" 
					   + homeTeam.getName() + " " + homeTeamPredictedScore + " (" + homeTeamScore + " - " + homePlayersPlayed + "/" + homeSelectedSize + " played)"
					   + resultString 
					   + awayTeam.getName() + " " + awayTeamPredictedScore + " (" + awayTeamScore + " - " + awayPlayersPlayed + "/" + awaySelectedSize + " played)"
					   + "</li>\n";
			
			currentLadderScores.put(fixture.getHomeTeam(), homeTeamPredictedScore);
			currentLadderScores.put(fixture.getAwayTeam(), awayTeamPredictedScore);
		}
		
		body = body + "</ul></p>\n";
		
		if(round <= 18) {
			loggerUtils.log("info", "Calculating Live Ladder");
			LadderCalculatorHandler ladderCalculator = new LadderCalculatorHandler();
			ladderCalculator.configureLogging(mdcKey, loggerName, logfile);
			ladderCalculator.execute(round, true);
			
			List<DflLadder> ladder = dflLadderService.getLadderForRound(round);
			Collections.sort(ladder, Collections.reverseOrder());
									
			body = body + "<p>Live Ladder:</p>\n";
			body = body + "<p><table border=1 style=\"border-collapse: collapse; border: 1px solid black;\">\n";
			body = body + "<tr>\n";
			body = body + "<th align=left style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">Team</th>"
					    + "<th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">Pts</th><th style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%</th>";
			body = body + "</tr>\n";
			
			String tableFormat = "<td style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%s</td>"
					           + "<td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%d</td><td align=right style=\"border: 1px solid black; padding: 1px 5px 1px 5px;\">%.2f</td>"
					           + "%n";
			
			for(DflLadder team : ladder) {
				String teamName = dflTeamService.get(team.getTeamCode()).getName();
				
				body = body + "<tr>\n";
				body = body + String.format(tableFormat, teamName, team.getPts(), team.getPercentage());
				body = body + "</tr>\n";
			}
			
			body = body + "</table></p>\n";
		}
		body = body + "<p>Results attached.</p>\n";
		body = body + "<p>DFL Manager Admin</p>\n";
		body = body + "</div></body></html>";
		
		return body;
	}
	
private String selectionSummary(int round, List<DflTeam> teams) {
		
		String text = "<br><p><b>Selection Summary</b></p>\n";
		
		for(DflTeam team : teams) {
			List<InsAndOuts> insAndOuts = insAndOutsService.getByTeamAndRound(round, team.getTeamCode());
			List<InsAndOuts> ins = new ArrayList<>();
			List<InsAndOuts> outs = new ArrayList<>();
			
			InsAndOuts emg1 = null;
			InsAndOuts emg2 = null;
			
			for(InsAndOuts selection : insAndOuts) {
				switch(selection.getInOrOut()) {
					case "I" : ins.add(selection); break;
					case "O" : outs.add(selection); break;
					case "E1" : emg1 = selection; break;
					case "E2" : emg2 = selection; break;
				}
			}
			
			text = text + "<p><b>" + team.getShortName() + ":</b>\n";
			
			if(ins.isEmpty() && outs.isEmpty() && emg1 == null && emg2 == null) {
				text = text + "No selections received<br>\n";
			} else {
				text = text + "<br>\n";

				if(!ins.isEmpty()) {
					String line = "";
					
					text = text + "<b>Ins: </b>\n";
					
					for(InsAndOuts in : ins) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), in.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());
						
						if(line.length() == 0) {
							line = in.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + in.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					
					text = text + line + "<br>\n";
				}
				if(!outs.isEmpty()) {
					String line = "";
					
					text = text + "<b>Outs</b>\n";
					
					for(InsAndOuts out : outs) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), out.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());

						if(line.length() == 0) {
							line = out.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + out.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					
					text = text + line + "<br>\n";
				}
				if(emg1 != null && emg2 != null) {
					String line = "";
					
					text = text + "<b>Emgs</b>\n";
					if(emg1 != null) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), emg1.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());

						if(line.length() == 0) {
							line = emg1.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + emg2.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					if(emg2 != null) {
						DflTeamPlayer teamPlayer = dflTeamPlayerService.getTeamPlayerForTeam(team.getTeamCode(), emg2.getTeamPlayerId());
						DflPlayer player = dflPlayerService.get(teamPlayer.getPlayerId());

						if(line.length() == 0) {
							line = emg2.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						} else {
							line = line + ", " + emg2.getTeamPlayerId() + " " + player.getFirstName() + " " + player.getLastName() + " " + player.getAflClub() + " " + player.getPosition();
						}
					}
					
					text = text + line + "<br>\n";
				}
			}
			
			text = text + "</p>\n";
		}
		
		text = text + "<br>\n";
		
		return text;
	}
	
	public static void main(String[] args) {
		
		try {
			String email = null;
			int round = 0;
			boolean isFinal = false;
			
			if(args.length > 3 || args.length < 1) {
				System.out.println("usage: RawStatsReport <round> optional [Final <email>]");
			} else {
				
				round = Integer.parseInt(args[0]);
				
				if(args.length == 2) {
					if(args[1].equalsIgnoreCase("Final")) {
						isFinal = true;
					} else {
						email = args[1];
					}
				} else if(args.length == 3) {
					if(args[1].equalsIgnoreCase("Final")) {
						isFinal = true;
						email = args[2];
					} else if(args[2].equalsIgnoreCase("Final")) {
						isFinal = true;
						email = args[1];
					} else {
						System.out.println("usage: RawStatsReport <round> optional [Final <email>]");
					}
				}
				
				//JndiProvider.bind();
				
				ResultsReport report = new ResultsReport();
				report.configureLogging("batch.name", "batch-logger", "ResultsReport");
				report.execute(round, isFinal, email);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
