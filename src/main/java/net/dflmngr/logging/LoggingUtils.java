package net.dflmngr.logging;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtils {
	
	private Logger logger;
	private String process;

	public LoggingUtils(String process) {
		this.process = process;

		boolean logToSyslog = Boolean.parseBoolean(System.getenv().getOrDefault("LOG_TO_SYSLOG", "false"));
		
		if(logToSyslog) {
			logger = LoggerFactory.getLogger("stdout-with-syslog-logger");
		} else {
			logger = LoggerFactory.getLogger("stdout-logger");
		}
	}
	
	public void log(String level, String msg, Object...arguments) {

		String callingClass = Thread.currentThread().getStackTrace()[2].getClassName();
		String callingClassShort = callingClass.substring(callingClass.lastIndexOf(".")+1, callingClass.length());
		String callingMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
		int lineNo = Thread.currentThread().getStackTrace()[2].getLineNumber();
		
		String loggerMsg = "[" + process + "]" + "[" + callingClassShort + "." +  callingMethod + "(Line:" + lineNo +")] - " + msg;

		try {
			switch (level) {
				case "info" : logger.info(loggerMsg, arguments); break;
				case "error" : logger.error(loggerMsg, arguments); break;
				default : logger.debug(loggerMsg, arguments); break;
			}
		} catch (Exception ex) {
			logger.error("Error in ... ", ex);
		}
	}

	public void logException(String msg, Throwable ex) {		
		try {
			logger.error(msg, ex);
			String stacktrace = ExceptionUtils.getStackTrace(ex);
			logger.error(stacktrace);
		} catch (Exception intEx) {
			logger.error("Error in ... ", intEx);
		} 
	}
}