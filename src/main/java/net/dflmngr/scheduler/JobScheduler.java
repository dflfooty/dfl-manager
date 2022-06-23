package net.dflmngr.scheduler;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.quartz.CronExpression;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.utils.DflmngrUtils;

public class JobScheduler {
	
	private static final LoggingUtils loggerUtils = new LoggingUtils("Scheduler");
	
	Scheduler scheduler;
	
	public JobScheduler() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
        	@Override
            public void run() {
                loggerUtils.log("info", "---- Shutting down DFL Manager Scheduler ----");
            	if(scheduler != null) {
            		try {
						scheduler.shutdown();
					} catch (Exception e) {
						e.printStackTrace();
					}
            	}
            }   
        });
	}
	
	public void execute() throws IOException, SchedulerException {			
		loggerUtils.log("info", "---- Starting DFL Manager Scheduler ----");
		
		Properties schedulerProperties = getSchedulerConfig();
		
		loggerUtils.log("info", "DFL Manager scheduler config: {}", schedulerProperties);
		
		StdSchedulerFactory factory = new StdSchedulerFactory();
		factory.initialize(schedulerProperties);
		scheduler = factory.getScheduler();

		scheduler.start();

		loggerUtils.log("info", "---- Running DFL Manager Scheduler ----");
	}
	
		
	private static Properties getSchedulerConfig() throws IOException {
		
		Properties schedulerProperties = new Properties();
		
		InputStream stream = JobScheduler.class.getResourceAsStream("/scheduler.properties");
		schedulerProperties.load(stream);
		
		schedulerProperties.setProperty("org.quartz.dataSource.dflmngrDB.URL", System.getenv("JDBC_DATABASE_URL"));
		
		return schedulerProperties;	
	}
		
	public static void schedule(String jobName, String jobGroup, String jobClassStr, 
	                            Map<String, Object> jobParams, String cronStr, boolean isImmediate)  {
		try {			
			String now = DflmngrUtils.getNowStr();
			String jobNameKey;
			String jobTriggerKey;
			
			loggerUtils.log("info", "Schedule job: {}", jobName);
						
			if(isImmediate) {
				jobNameKey = jobName + "_immediate_" + now;
				jobTriggerKey = jobName + "_trigger_immediate_" + now;
				createAndSchedule(jobNameKey, jobGroup, jobClassStr, jobTriggerKey, jobParams, cronStr, true);
			}
			
			if(cronStr != null && !cronStr.equals("")) {
				jobNameKey = jobName + "_" + now;
				jobTriggerKey = jobName + "_trigger_" + now;
				createAndSchedule(jobNameKey, jobGroup, jobClassStr, jobTriggerKey, jobParams, cronStr, false);
			}
			
			loggerUtils.log("info", "Scheduled job: {}", jobName);
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}

	public static void deleteGroup(String group) throws IOException, SchedulerException {
		loggerUtils.log("info", "Deleting scheduler group: group={}", group);

		Properties schedulerProperties = getSchedulerConfig();
		StdSchedulerFactory factory = new StdSchedulerFactory();
		factory.initialize(schedulerProperties);
		Scheduler scheduler = factory.getScheduler();

		List<JobKey> jobKeys = List.copyOf(scheduler.getJobKeys(GroupMatcher.groupEquals(group)));

		if(jobKeys.isEmpty()) {
			loggerUtils.log("info", "No job keys in group");
		} else {
			loggerUtils.log("info", "Group keys to delete: keys={}", jobKeys);
			scheduler.deleteJobs(jobKeys);
		}

		if(!scheduler.isShutdown()) {
			scheduler.shutdown();
		}
	}
	
	private static void createAndSchedule(String jobNameKey, String group, String jobClassStr, String jobTriggerKey, Map<String, Object> jobParams, String cronStr, boolean isImmediate) throws ClassNotFoundException {
		loggerUtils.log("info", "Final job details: jobNameKey={}; group={}; jobClassStr={}; jobTriggerKey={}; jobParams={}; cronStr={}; isImmediate={};", jobNameKey, group, jobClassStr, jobTriggerKey, jobParams, cronStr, isImmediate);
		
		Class<? extends Job> jobClass = Class.forName(jobClassStr).asSubclass(Job.class);
		
		JobDetail job = null;
		Trigger trigger = null;
		
		try {
			job = newJob(jobClass).withIdentity(jobNameKey, group).build();
			if(jobParams != null && !jobParams.isEmpty()) {
				job.getJobDataMap().putAll(jobParams);
			}
			
			if(isImmediate) {
				trigger = newTrigger().withIdentity(jobTriggerKey, group).startNow().forJob(job).build();
			} else {
				boolean valid = CronExpression.isValidExpression(cronStr);
				if(valid) {
					CronExpression cronExpression = new CronExpression(cronStr);
					cronExpression.setTimeZone(TimeZone.getTimeZone(DflmngrUtils.DEFAULT_TIMEZONE));
					Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(DflmngrUtils.DEFAULT_TIMEZONE));
					Date currentDate = calendar.getTime();
					valid = cronExpression.getNextValidTimeAfter(currentDate) != null;
				}
				if(valid) {
					trigger = newTrigger().withIdentity(jobTriggerKey, group).withSchedule(cronSchedule(cronStr).inTimeZone(TimeZone.getTimeZone(DflmngrUtils.DEFAULT_TIMEZONE))).forJob(job).build();
				}
			}

			if(trigger != null) {
				Properties schedulerProperties = getSchedulerConfig();
				StdSchedulerFactory factory = new StdSchedulerFactory();
				factory.initialize(schedulerProperties);
				Scheduler scheduler = factory.getScheduler();
				scheduler.scheduleJob(job, trigger);

				if(!scheduler.isShutdown()) {
					scheduler.shutdown();
				}
			} else {
				loggerUtils.log("info", "Job not scheduled as it will not run");
			}
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}
	
	public static void main(String[] args) {
		JobScheduler jobScheduler = new JobScheduler();
		try {
			jobScheduler.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}