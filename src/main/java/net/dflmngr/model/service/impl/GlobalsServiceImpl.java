package net.dflmngr.model.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dflmngr.model.dao.GlobalsDao;
import net.dflmngr.model.dao.impl.GlobalsDaoImpl;
import net.dflmngr.model.entity.Globals;
import net.dflmngr.model.entity.keys.GlobalsPK;
import net.dflmngr.model.service.GlobalsService;

public class GlobalsServiceImpl extends GenericServiceImpl<Globals, GlobalsPK>implements GlobalsService {

	GlobalsDao dao;

	public GlobalsServiceImpl() {
		dao = new GlobalsDaoImpl();
		super.setDao(dao);
	}

	public Map<String, String> getValueAndParams(String code, String groupCode) {

		Map<String, String> valueAndParams = new HashMap<>();

		GlobalsPK pk = new GlobalsPK();
		pk.setCode(code);
		pk.setGroupCode(groupCode);

		Globals globals = dao.findById(pk);

		valueAndParams.put("value", globals.getValue());
		valueAndParams.put("params", globals.getParams());

		return valueAndParams;
	}

	public String getValue(String code, String groupCode) {

		String value = "";

		GlobalsPK pk = new GlobalsPK();
		pk.setGroupCode(groupCode);
		pk.setCode(code);

		Globals globals = dao.findById(pk);

		if(globals != null) {
			value = globals.getValue();
		}

		return value;
	}

	public List<String> getCodes(String groupCode) {

		List<String> codes = new ArrayList<>();

		List<Globals> globalsList = dao.findCodesForGroup(groupCode);

		for(Globals globals : globalsList) {
			codes.add(globals.getCode());
		}

		return codes;
	}

	public Map<String, String> getGroupValues(String groupCode) {

		Map<String, String> codesValues = new HashMap<>();

		List<Globals> globalsList = dao.findCodesForGroup(groupCode);

		for(Globals globals : globalsList) {
			codesValues.put(globals.getCode(), globals.getValue());
		}

		return codesValues;
	}

	public String getCurrentYear() {

		String currentYear = "";
		String code = "currentYear";
		String groupCode = "dflRef";

		currentYear = getValue(code, groupCode);

		return currentYear;
	}

	public List<String> getAflFixtureUrl() {

		List<String> aflFixtureUrl = new ArrayList<>();
		String code = "aflFixtureUrl";
		String groupCode = "aflRef";

		Map<String, String> valueAndParams = getValueAndParams(code, groupCode);

		aflFixtureUrl.add(valueAndParams.get("value"));

		if(valueAndParams.containsKey("params")) {
			String params = valueAndParams.get("params");
			String[] parts = params.split(";");

			aflFixtureUrl.addAll(Arrays.asList(parts));
		}

		return aflFixtureUrl;
	}

	public String getGroundTimeZone(String ground) {

		String timezone = "";
		String code = ground;
		String groupCode = "timezone";

		timezone = getValue(code, groupCode);

		if(timezone.equals("")) {
			code = "default";
			timezone = getValue(code, groupCode);
		}

		return timezone;
	}

	public Map<String, String> getGround(String groundName) {

		Map<String, String> ground = new HashMap<>();

		String code = groundName;
		String groupCode = "ground";

		Map<String, String> data = getValueAndParams(code, groupCode);

		ground.put("ground", data.get("value"));
		ground.put("timezone", data.get("params"));

		return ground;
	}

	public List<String> getTeamCodes() {

		String groupCode = "teamCode";
		List<String> teamCodes = getCodes(groupCode);

		return teamCodes;
	}

	public String getAppDir() {

		String appDir = "";
		String code = "appDir";
		String groupCode = "dflRef";

		appDir = getValue(code, groupCode);

		return appDir;
	}

	public String getReportDir() {

		String reportDir = "";
		String code = "reportDir";
		String groupCode = "dflRef";

		reportDir = getValue(code, groupCode);

		return reportDir;
	}

	public String getStandardLockoutTime() {

		String standardLockoutTime = "";
		String code = "standardLockoutTime";
		String groupCode = "dflRef";

		standardLockoutTime = getValue(code, groupCode);

		return standardLockoutTime;
	}

	public String getNonStandardLockout(int round) {

		String nonStandardLockout = "";
		String code = Integer.toString(round);
		String groupCode = "nonStandardLockout";

		nonStandardLockout = getValue(code, groupCode);

		return nonStandardLockout;
	}

	public String getAflRoundsMax() {

		String aflRoundsMax = "";
		String code = "aflRoundsMax";
		String groupCode = "aflRef";

		aflRoundsMax = getValue(code, groupCode);

		return aflRoundsMax;
	}

	public Map<String, String> getEmailConfig() {

		Map<String, String> emailConfig = new HashMap<>();

		String emailParam = "";
		String code = "dflmngrEmailAddr";
		String groupCode = "dflRef";

		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		code = "dflgroupEmailAddr";
		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		code = "incomingMailHost";
		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		code = "incomingMailPort";
		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		code = "outgoingMailHost";
		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		code = "outgoingMailPort";
		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		code = "mailUsername";
		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		code = "mailPassword";
		emailParam = getValue(code, groupCode);
		emailConfig.put(code, emailParam);

		return emailConfig;
	}

	public String getAflStatsUrl() {

		String statsUrl = "";
		String code = "aflStatsUrl";
		String groupCode = "aflRef";

		statsUrl = getValue(code, groupCode);

		return statsUrl;
	}

	public String getEmailerRoot() {

		String emailerRoot = "";
		String code = "emailerRoot";
		String groupCode = "dflRef";

		emailerRoot = getValue(code, groupCode);

		return emailerRoot;
	}

	public String getTeamEmail(String teamCode) {

		String email = "";
		String code = teamCode;
		String groupCode = "teamEmail";

		email = getValue(code, groupCode);

		return email;
	}

	public String getCurrentRound() {

		String currentRound = "";
		String code = "currentRound";
		String groupCode = "dflRef";

		currentRound = getValue(code, groupCode);

		return currentRound;
	}

	public String getPreSeasonFixtureUrl() {

		String preSeasonFixtureUrl = "";
		String code = "preSeasonFixture";
		String groupCode = "aflRef";

		preSeasonFixtureUrl = getValue(code, groupCode);

		return preSeasonFixtureUrl;
	}

	public String getBrowserPath() {

		String browserPath = "";
		String code = "browserPath";
		String groupCode = "dflRef";

		browserPath = getValue(code, groupCode);

		return browserPath;
	}

	public int getWebdriverWait() {

		int webriverWait = 0;
		String code = "webdriverWait";
		String groupCode = "dflRef";

		webriverWait = Integer.parseInt(getValue(code, groupCode));

		return webriverWait;
	}

	public int getWebdriverTimeout() {

		int webdriverTimeout = 0;
		String code = "webdriverTimeout";
		String groupCode = "dflRef";

		webdriverTimeout = Integer.parseInt(getValue(code, groupCode));

		return webdriverTimeout;
	}

	public Map<String, String> getDraftOrder() {

		Map<String, String> draftOrder = null;
		String groupCode = "draftOrder";

		draftOrder = getGroupValues(groupCode);

		return draftOrder;
	}

	public void setCurrentRound(int newRound) {
		dao.beginTransaction();
		GlobalsPK pk = new GlobalsPK();
		pk.setCode("currentRound");
		pk.setGroupCode("dflRef");
		Globals currentRound = dao.findById(pk);
		currentRound.setValue(Integer.toString(newRound));
		dao.persist(currentRound);
		dao.commit();
	}

	public String getAflTeamMap(String team) {
		String teamDecode = "";

		String code = team;
		String groupCode = "aflTeamMap";

		teamDecode = getValue(code, groupCode);

		return teamDecode;

	}

	public Map<Integer, Map<Integer, String[]>> getDflFixuteTemplate() {
		Map<Integer, Map<Integer, String[]>> fixtureTemplate = new HashMap<>();

		String groupCode = "dflFixtureTemplate";

		Map<String, String> globalsTemplate = getGroupValues(groupCode);

		for (Map.Entry<String, String> entry : globalsTemplate.entrySet()) {
		    int round = Integer.parseInt(entry.getKey());
		    String roundTemplate = entry.getValue();

			String[] games = roundTemplate.substring(0, roundTemplate.length()-1).substring(1).split("\\]\\[");
			//String[] games = s2.split("][");

			Map<Integer, String[]> roundFixtures = new HashMap<>();

			System.out.print(round + " - ");

			for(int j = 0; j < games.length; j++) {
				String[] g = games[j].split(",");
				roundFixtures.put(j+1, g);
				System.out.print(j+1 + ": " + g[0] + " vs " + g[1] + " ");
			}
			System.out.print("\n");

			fixtureTemplate.put(round, roundFixtures);
		}

		return fixtureTemplate;
	}

	public Map<String, String> getDflFixtureOrder() {

		Map<String, String> fixtureOrder = null;
		String groupCode = "dflFixtureOrder";

		fixtureOrder = getGroupValues(groupCode);

		return fixtureOrder;
	}

	public boolean getSendMedalReports(int currentRound) {

		boolean cutoff = false;
		int roundCutoff = 0;
		String code = "medalsRoundCutoff";
		String groupCode = "dflRef";

		roundCutoff = Integer.parseInt(getValue(code, groupCode));

		if(currentRound > roundCutoff) {
			cutoff = true;
		}

		return cutoff;
	}

	public String getOnlineBaseUrl() {

		String onlineBaseUrl = "";
		String code = "onlineBaseUrl";
		String groupCode = "dflRef";

		onlineBaseUrl = getValue(code, groupCode);

		return onlineBaseUrl;
	}

	public int getUseAverage(String teamCode) {

		int round = 0;
		String value;
		String groupCode = "useAvg";

		value = getValue(teamCode, groupCode);

		if(value != null && value.length() > 0) {
			round = Integer.parseInt(value);
		}

		return round;
	}
}
