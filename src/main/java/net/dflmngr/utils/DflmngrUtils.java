package net.dflmngr.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;

public class DflmngrUtils {

	private DflmngrUtils() {}

	public static String getNowStr() {
		return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
	}

	public static final String DEFAULT_TIMEZONE;
	static
	{
			GlobalsService globalsService = new GlobalsServiceImpl();
			DEFAULT_TIMEZONE = globalsService.getGroundTimeZone("default");
	}

	public static final Map<String, Integer> weekDaysInt;
	static
	{
	    weekDaysInt = new HashMap<String, Integer>();
	    weekDaysInt.put("Monday", Calendar.MONDAY);
	    weekDaysInt.put("Tuesday", Calendar.TUESDAY);
	    weekDaysInt.put("Wednesday", Calendar.WEDNESDAY);
	    weekDaysInt.put("Thursday", Calendar.THURSDAY);
	    weekDaysInt.put("Friday", Calendar.FRIDAY);
	    weekDaysInt.put("Saturday", Calendar.SATURDAY);
	    weekDaysInt.put("Sunday", Calendar.SUNDAY);
	}

	public static final Map<Integer, String> weekDaysString;
	static
	{
	    weekDaysString = new HashMap<Integer, String>();
	    weekDaysString.put(Calendar.MONDAY, "Monday");
	    weekDaysString.put(Calendar.TUESDAY, "Tuesday");
	    weekDaysString.put(Calendar.WEDNESDAY, "Wednesday");
	    weekDaysString.put(Calendar.THURSDAY, "Thursday");
	    weekDaysString.put(Calendar.FRIDAY, "Friday");
	    weekDaysString.put(Calendar.SATURDAY, "Saturday");
	    weekDaysString.put(Calendar.SUNDAY, "Sunday");
	}

	public static final Map<String, Integer> AMPM;
	static
	{
	    AMPM = new HashMap<String, Integer>();
	    AMPM.put("AM", Calendar.AM);
	    AMPM.put("PM", Calendar.PM);
	}

	public static final Map<String, String> dflAflTeamMap;
	static
	{
		dflAflTeamMap = new HashMap<String, String>();
		dflAflTeamMap.put("Adel", "ADEL");
		dflAflTeamMap.put("Bris", "BL");
		dflAflTeamMap.put("Bull", "WB");
		dflAflTeamMap.put("Carl", "CARL");
		dflAflTeamMap.put("Coll", "COLL");
		dflAflTeamMap.put("Ess", "ESS");
		dflAflTeamMap.put("Freo", "FREO");
		dflAflTeamMap.put("Geel", "GEEL");
		dflAflTeamMap.put("Gold", "GCFC");
		dflAflTeamMap.put("GWS", "GWS");
		dflAflTeamMap.put("Haw", "HAW");
		dflAflTeamMap.put("Melb", "MELB");
		dflAflTeamMap.put("Nth", "NMFC");
		dflAflTeamMap.put("Port", "PORT");
		dflAflTeamMap.put("Rich", "RICH");
		dflAflTeamMap.put("StK", "STK");
		dflAflTeamMap.put("Syd", "SYD");
		dflAflTeamMap.put("WCE", "WCE");
	}

	public static final Map<String, String> aflDflTeamMap;
	static
	{
		aflDflTeamMap = new HashMap<String, String>();
		aflDflTeamMap.put("ADEL", "Adel");
		aflDflTeamMap.put("BL", "Bris");
		aflDflTeamMap.put("WB", "Bull");
		aflDflTeamMap.put("CARL", "Carl");
		aflDflTeamMap.put("COLL", "Coll");
		aflDflTeamMap.put("ESS", "Ess");
		aflDflTeamMap.put("FRE", "Freo");
		aflDflTeamMap.put("GEEL", "Geel");
		aflDflTeamMap.put("GCFC", "Gold");
		aflDflTeamMap.put("GWS", "GWS");
		aflDflTeamMap.put("HAW", "Haw");
		aflDflTeamMap.put("MELB", "Melb");
		aflDflTeamMap.put("NMFC", "Nth");
		aflDflTeamMap.put("PORT", "Port");
		aflDflTeamMap.put("RICH", "Rich");
		aflDflTeamMap.put("STK", "StK");
		aflDflTeamMap.put("SYD", "Syd");
		aflDflTeamMap.put("WCE", "WCE");
	}

}