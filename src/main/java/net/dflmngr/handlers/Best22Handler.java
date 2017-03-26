package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflBest22;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflPlayerScores;
import net.dflmngr.model.service.DflBest22Service;
import net.dflmngr.model.service.DflPlayerScoresService;
import net.dflmngr.model.service.DflPlayerService;
import net.dflmngr.model.service.impl.DflBest22ServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerScoresServiceImpl;
import net.dflmngr.model.service.impl.DflPlayerServiceImpl;

public class Best22Handler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "Best22Handler";
	
	String mdcKey;
	String loggerName;
	String logfile;
	
	DflPlayerScoresService dflPlayerScoresService;
	DflPlayerService dflPlayerService;
	DflBest22Service dflBest22Service;
	
	public Best22Handler() {
		dflPlayerScoresService = new DflPlayerScoresServiceImpl();
		dflPlayerService = new DflPlayerServiceImpl();
		dflBest22Service = new DflBest22ServiceImpl();
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(int round) {
		
		try{
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "Best22Handler excuting, rount={} ....", round);
			
			calculateBest22(round);
			
			dflPlayerScoresService.close();
			
			loggerUtils.log("info", "Best22Handler complete");
			
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	private void calculateBest22(int round) {
		
		Map<Integer, List<DflPlayerScores>> allPlayerScores = dflPlayerScoresService.getUptoRoundWithKey(round);
		
		Map<Integer, Integer> ffScores = new HashMap<>();
		Map<Integer, Integer> fwdScores = new HashMap<>();
		Map<Integer, Integer> rckScores = new HashMap<>();
		Map<Integer, Integer> midScores = new HashMap<>();
		Map<Integer, Integer> fbScores = new HashMap<>();
		Map<Integer, Integer> defScores = new HashMap<>();
		
		for (Map.Entry<Integer,  List<DflPlayerScores>> entry : allPlayerScores.entrySet()) {
			Integer playerId = entry.getKey();
			List<DflPlayerScores> playerScores = entry.getValue();
			
			int total = 0;
			
			for(DflPlayerScores score : playerScores) {
				total = total + score.getScore();
			}
			
			DflPlayer player = dflPlayerService.get(playerId);
			String position = player.getPosition();
			
			switch(position) {
				case "FF": ffScores.put(playerId, total); break;
				case "Fwd": fwdScores.put(playerId, total); break;
				case "Rck": rckScores.put(playerId, total); break;
				case "Mid": midScores.put(playerId, total); break;
				case "FB": fbScores.put(playerId, total); break;
				case "Def": defScores.put(playerId, total); break;
			}
		}
		
		ffScores = sortByValue(ffScores);
		fwdScores = sortByValue(fwdScores);
		rckScores = sortByValue(rckScores);
		midScores = sortByValue(midScores);
		fbScores = sortByValue(fbScores);
		defScores = sortByValue(defScores);
		
		Map<Integer, Integer> best22selections = new HashMap<>();
		
		loggerUtils.log("info", "Selecting Full Forward");
		best22selections.putAll(selectPlayers(ffScores, "FF"));
		loggerUtils.log("info", "Selecting Fowards");
		best22selections.putAll(selectPlayers(ffScores, "Fwd"));
		loggerUtils.log("info", "Selecting Ruck");
		best22selections.putAll(selectPlayers(ffScores, "Rck"));
		loggerUtils.log("info", "Selecting Midfielders");
		best22selections.putAll(selectPlayers(ffScores, "Mid"));
		loggerUtils.log("info", "Selecting Full Back");
		best22selections.putAll(selectPlayers(ffScores, "FB"));
		loggerUtils.log("info", "Selecting Defenders");
		best22selections.putAll(selectPlayers(ffScores, "Def"));
		
		Map<Integer, Integer> benchScores = new HashMap<>();
		Map.Entry<Integer, Integer> benchSelection = ffScores.entrySet().iterator().next();
		benchScores.put(benchSelection.getKey(), benchSelection.getValue());
		benchSelection = fwdScores.entrySet().iterator().next();
		benchScores.put(benchSelection.getKey(), benchSelection.getValue());
		benchSelection = rckScores.entrySet().iterator().next();
		benchScores.put(benchSelection.getKey(), benchSelection.getValue());
		benchSelection = midScores.entrySet().iterator().next();
		benchScores.put(benchSelection.getKey(), benchSelection.getValue());
		benchSelection = fbScores.entrySet().iterator().next();
		benchScores.put(benchSelection.getKey(), benchSelection.getValue());
		benchSelection = defScores.entrySet().iterator().next();
		benchScores.put(benchSelection.getKey(), benchSelection.getValue());
		
		List<DflBest22> best22 = new ArrayList<>();
		
		for(Map.Entry<Integer, Integer> entry : best22selections.entrySet()) {
			int playerId = entry.getKey();
			int score = entry.getValue();
			
			DflBest22 best22player = new DflBest22();
			best22player.setPlayerId(playerId);
			best22player.setRound(round);
			best22player.setScore(score);
			best22player.setBench(false);
			
			best22.add(best22player);
		}
		
		best22selections.clear();
		
		benchScores = sortByValue(benchScores);
		
		loggerUtils.log("info", "Selecting Bench");
		best22selections.putAll(selectPlayers(benchScores, "Bench"));
		
		for(Map.Entry<Integer, Integer> entry : best22selections.entrySet()) {
			int playerId = entry.getKey();
			int score = entry.getValue();
			
			DflBest22 best22player = new DflBest22();
			best22player.setPlayerId(playerId);
			best22player.setRound(round);
			best22player.setScore(score);
			best22player.setBench(true);
			
			best22.add(best22player);
		}
		
		loggerUtils.log("info", "Saving best 22 to database");
		dflBest22Service.insertAll(best22, false);
	}
	
	private Map<Integer, Integer> selectPlayers(Map<Integer, Integer> players, String position) {
		Map<Integer, Integer> selectedPlayers = new HashMap<>();
		
		int selectionCount = 0;
		
		switch(position) {
			case "FF": selectionCount = 1;
			case "Fwd": selectionCount = 5;
			case "Rck": selectionCount = 1;
			case "Mid": selectionCount = 5;
			case "FB": selectionCount = 1;
			case "Def": selectionCount = 5;
			case "Bench": selectionCount = 4;
		}

		for(int i = 1; i < selectionCount; i++) {
			Map.Entry<Integer, Integer> selection = players.entrySet().iterator().next();
			loggerUtils.log("info", "Selecting: {}", selection);
			selectedPlayers.put(selection.getKey(), selection.getValue());
			players.remove(selection.getKey());
		}
				
		return selectedPlayers;		
	}
	
	private Map<Integer, Integer> sortByValue(Map<Integer, Integer> unsortedMap) {
		Comparator<Integer> comparator = new ValueComparator<Integer, Integer>(unsortedMap);
		TreeMap<Integer, Integer> sortedMap = new TreeMap<Integer, Integer>(comparator);
		sortedMap.putAll(unsortedMap);
		return sortedMap;
	}
}

class ValueComparator<K, V extends Comparable<V>> implements Comparator<K>{
	 
	Map<K, V> map = new HashMap<>();
 
	public ValueComparator(Map<K, V> map){
		this.map.putAll(map);
	}
 
	@Override
	public int compare(K s1, K s2) {
		return -map.get(s1).compareTo(map.get(s2));//descending order	
	}
}
