package net.dflmngr.handlers;

import java.util.ArrayList;
import java.util.List;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.DomainDecodes;
import net.dflmngr.model.entity.DflEarlyInsAndOuts;
import net.dflmngr.model.entity.InsAndOuts;
import net.dflmngr.model.service.DflEarlyInsAndOutsService;
import net.dflmngr.model.service.InsAndOutsService;
import net.dflmngr.model.service.impl.DflEarlyInsAndOutsServiceImpl;
import net.dflmngr.model.service.impl.InsAndOutsServiceImpl;

public class TeamInsOutsLoaderHandler {
	private LoggingUtils loggerUtils;
	
	boolean isExecutable;
	
	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "Selections";
	
	String mdcKey;
	String loggerName;
	String logfile;
		
	public TeamInsOutsLoaderHandler() throws Exception {		
		
		isExecutable = false;
	}
	
	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		//loggerUtils = new LoggingUtils(loggerName, mdcKey, logfile);
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}
	
	public void execute(String teamCode, int round, List<Integer> ins, List<Integer> outs, List<Double> emgs, boolean earlyGames) {
		
		try {
			if(!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}
			
			loggerUtils.log("info", "Processing ins and out selections for: teamCode={}; round={}; ins={}; outs={}; emergencies={}", teamCode, round, ins, outs, emgs);
			
			if(earlyGames) {
				loggerUtils.log("info", "Early Games, saving to early games ins and outs");
				
				DflEarlyInsAndOutsService dflEarlyInsAndOutsService = new DflEarlyInsAndOutsServiceImpl();
				List<DflEarlyInsAndOuts> earlyInsAndOuts = new ArrayList<>();
				
				for(Integer i : ins) {
					DflEarlyInsAndOuts in = new DflEarlyInsAndOuts();
					in.setRound(round);
					in.setTeamCode(teamCode);
					in.setTeamPlayerId(i);
					in.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN);
					
					earlyInsAndOuts.add(in);
				}
				
				for(Integer o : outs) {
					DflEarlyInsAndOuts out = new DflEarlyInsAndOuts();
					out.setRound(round);
					out.setTeamCode(teamCode);
					out.setTeamPlayerId(o);
					out.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT);
					
					earlyInsAndOuts.add(out);
				}
				
				for(double e : emgs) {
					DflEarlyInsAndOuts emg = new DflEarlyInsAndOuts();
					emg.setRound(round);
					emg.setTeamCode(teamCode);
					
					int eid = (int) e;
					emg.setTeamPlayerId(eid);
					
					int e1e2 = Integer.parseInt(Double.toString(e).split("\\.")[1].substring(0, 1));
					//double e1e2 = Math.floor((e - eid) * 100) / 100;
					//if(e1e2 == 0.1) {
					if(e1e2 == 1) {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1);
					} else {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG2);
					}
					
					earlyInsAndOuts.add(emg);
				}
				
				loggerUtils.log("info", "Saving early ins and outs to database: ", earlyInsAndOuts);
				dflEarlyInsAndOutsService.saveTeamInsAndOuts(earlyInsAndOuts);
				loggerUtils.log("info", "Ins and outs saved");
				
			} else {
				loggerUtils.log("info", "Not early games, saving to regular ins and outs");
				
				InsAndOutsService insAndOutsService = new InsAndOutsServiceImpl();
				List<InsAndOuts> insAndOuts = new ArrayList<>();
				
				for(Integer i : ins) {
					InsAndOuts in = new InsAndOuts();
					in.setRound(round);
					in.setTeamCode(teamCode);
					in.setTeamPlayerId(i);
					in.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.IN);
					
					insAndOuts.add(in);
				}
				
				for(Integer o : outs) {
					InsAndOuts out = new InsAndOuts();
					out.setRound(round);
					out.setTeamCode(teamCode);
					out.setTeamPlayerId(o);
					out.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.OUT);
					
					insAndOuts.add(out);
				}
				
				for(double e : emgs) {
					InsAndOuts emg = new InsAndOuts();
					emg.setRound(round);
					emg.setTeamCode(teamCode);
					
					int eid = (int) e;
					emg.setTeamPlayerId(eid);
					
					
					int e1e2 = Integer.parseInt(Double.toString(e).split("\\.")[1].substring(0, 1));
					//double e1e2 = Math.floor((e - eid) * 100) / 100;
					//if(e1e2 == 0.1) {
					if(e1e2 == 1) {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG1);
					} else {
						emg.setInOrOut(DomainDecodes.INS_AND_OUTS.IN_OR_OUT.EMG2);
					}
					
					insAndOuts.add(emg);
				}
				
				loggerUtils.log("info", "Saving ins and outs to database: ", insAndOuts);
				insAndOutsService.saveTeamInsAndOuts(insAndOuts);
				loggerUtils.log("info", "Ins and outs saved");
				
				insAndOutsService.close();
			}
						
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
}