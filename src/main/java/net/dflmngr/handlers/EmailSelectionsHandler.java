package net.dflmngr.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import net.dflmngr.logging.LoggingUtils;
import net.dflmngr.model.entity.DflPlayer;
import net.dflmngr.model.entity.DflTeam;
import net.dflmngr.model.entity.DflTeamPlayer;
import net.dflmngr.model.service.DflTeamPlayerService;
import net.dflmngr.model.service.DflTeamService;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.DflTeamPlayerServiceImpl;
import net.dflmngr.model.service.impl.DflTeamServiceImpl;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.utils.DflmngrUtils;
import net.dflmngr.utils.oauth2.OAuth2Authenticator;
import net.dflmngr.validation.SelectedTeamValidation;
import net.freeutils.tnef.Attachment;
import net.freeutils.tnef.TNEFInputStream;

public class EmailSelectionsHandler {
	private LoggingUtils loggerUtils;

	boolean isExecutable;

	String defaultMdcKey = "batch.name";
	String defaultLoggerName = "batch-logger";
	String defaultLogfile = "Selections";

	String mdcKey;
	String loggerName;
	String logfile;

	private String dflmngrEmailAddr;
	private String incomingMailHost;
	private int incomingMailPort;
	private String outgoingMailHost;
	private int outgoingMailPort;
	private String mailUsername;
	private String mailPassword;

	private String emailOveride;

	GlobalsService globalsService;
	DflTeamService dflTeamService;
	DflTeamPlayerService dflTeamPlayerService;

	boolean selectionsFileAttached;

	List<SelectedTeamValidation> validationResults;
	Map<String, String> selectionsIdsCurrent;

	public EmailSelectionsHandler() {
		globalsService = new GlobalsServiceImpl();
		dflTeamService = new DflTeamServiceImpl();
		dflTeamPlayerService = new DflTeamPlayerServiceImpl();

		isExecutable = false;
	}

	public void configureLogging(String mdcKey, String loggerName, String logfile) {
		loggerUtils = new LoggingUtils(logfile);
		this.mdcKey = mdcKey;
		this.loggerName = loggerName;
		this.logfile = logfile;
		isExecutable = true;
	}

	public void execute() {
		try {
			if (!isExecutable) {
				configureLogging(defaultMdcKey, defaultLoggerName, defaultLogfile);
				loggerUtils.log("info", "Default logging configured");
			}

			validationResults = new ArrayList<>();
			selectionsIdsCurrent = new HashMap<>();

			loggerUtils.log("info", "Email Selections Handler is executing ....");

			Map<String, String> emailConfig = globalsService.getEmailConfig();

			this.dflmngrEmailAddr = emailConfig.get("dflmngrEmailAddr");
			this.incomingMailHost = emailConfig.get("incomingMailHost");
			this.incomingMailPort = Integer.parseInt(emailConfig.get("incomingMailPort"));
			this.outgoingMailHost = emailConfig.get("outgoingMailHost");
			this.outgoingMailPort = Integer.parseInt(emailConfig.get("outgoingMailPort"));
			this.mailUsername = emailConfig.get("mailUsername");
			this.mailPassword = emailConfig.get("mailPassword");

			this.emailOveride = "";

			if (!System.getenv("ENV").equals("production")) {
				this.dflmngrEmailAddr = System.getenv("DFL_MNGR_EMAIL");
				this.mailUsername = System.getenv("DFL_MNGR_EMAIL");
				this.emailOveride = System.getenv("EMAIL_OVERIDE");
			}

			loggerUtils.log("info",
					"Email config: dflmngrEmailAddr={}; incomingMailHost={}; incomingMailPort={}; outgoingMailHost={}; outgoingMailHost={}; mailUsername={}; mailPassword={}",
					dflmngrEmailAddr, incomingMailHost, incomingMailPort, outgoingMailHost, outgoingMailPort,
					mailUsername, mailPassword);

			OAuth2Authenticator.initialize();

			processSelections();

			loggerUtils.log("info", "Sending responses");

			sendResponses();

			globalsService.close();
			dflTeamService.close();
			dflTeamPlayerService.close();

			loggerUtils.log("info", "Email Selections Handler Completed");
		} catch (Exception ex) {
			loggerUtils.log("error", "Error in ... ", ex);
		}
	}

	private void processSelections() throws Exception {

		Properties properties = new Properties();
		properties.setProperty("mail.imap.ssl.enable", "true");

		Session session = Session.getInstance(properties);
		Store store = session.getStore("imap");
		store.connect(incomingMailHost, incomingMailPort, mailUsername, mailPassword);

		Folder inbox = store.getFolder("Inbox");
		inbox.open(Folder.READ_WRITE);

		Message[] messages = inbox.getMessages();

		loggerUtils.log("info", "Opended inbox: messages={}", messages.length);

		for (int i = 0; i < messages.length; i++) {

			loggerUtils.log("info", "Handling message {}", i);

			SelectedTeamValidation validationResult = null;

			selectionsFileAttached = false;

			try {

				Message message = messages[i];
				String from = InternetAddress.toString(message.getFrom());
				Instant instant = message.getReceivedDate().toInstant();
				ZonedDateTime receivedDate = ZonedDateTime.ofInstant(instant, ZoneId.of(DflmngrUtils.defaultTimezone));


				if(message.isMimeType("multipart/*")) {
					Multipart multipart = (Multipart) message.getContent();

					for (int j = 0; j < multipart.getCount(); j++) {
						BodyPart part = multipart.getBodyPart(j);
						validationResult = scanEmailPartsAndValidate(part, receivedDate, from);
						if (validationResult != null) {
							break;
						}
					}
				} else if(message.isMimeType("text/plain")) {
					validationResult = handleTextEmailContent(message.getContent().toString(), from);
				} else if(message.isMimeType("text/html")) {
					validationResult = handleHtmlEmailContent(message.getContent().toString(), from);
				}

				if (validationResult == null) {
					validationResult = new SelectedTeamValidation();
					loggerUtils.log("info", "Selection file or selection body is missing.");
					validationResult.selectionFileMissing = true;
					validationResult.setFrom(from);
					validationResults.add(validationResult);
					loggerUtils.log("info", "Message from {} ... FAILURE!", from);
				} else {
					if (validationResult.isValid()) {
						TeamInsOutsLoaderHandler selectionsLoader = new TeamInsOutsLoaderHandler();
						selectionsLoader.configureLogging(mdcKey, loggerName, logfile);

						if (validationResult.earlyGames) {
							loggerUtils.log("info",
									"Early Games any validation error is a warning .... Saving ins and outs to early tables in DB");
							selectionsLoader.execute(validationResult.getTeamCode(), validationResult.getRound(),
									validationResult.getInsAndOuts().get("in"),
									validationResult.getInsAndOuts().get("out"), validationResult.getEmergencies(),
									true);
						} else {
							loggerUtils.log("info", "Team selection is VALID.... Saving ins and outs to DB");
							selectionsLoader.execute(validationResult.getTeamCode(), validationResult.getRound(),
									validationResult.getInsAndOuts().get("in"),
									validationResult.getInsAndOuts().get("out"), validationResult.getEmergencies(),
									false);
						}
					} else {
						loggerUtils.log("info", "Team selection is invalid ... No changes made.");
					}
				}
			} catch (Exception ex) {
				loggerUtils.log("error", "Error in ... ", ex);
				try {
					String from = InternetAddress.toString(messages[i].getFrom());
					validationResult = new SelectedTeamValidation();
					validationResult.unknownError = true;
					validationResult.selectionFileMissing = false;
					validationResult.roundCompleted = false;
					validationResult.lockedOut = false;
					validationResult.setFrom(from);
					validationResults.add(validationResult);
					loggerUtils.log("info", "Message from {} ... FAILURE with EXCEPTION!", from);
				} catch (MessagingException ex2) {
					loggerUtils.log("error", "Error in ... ", ex2);
				}
			}
		}

		loggerUtils.log("info", "Moving messages to Processed folder");
		Folder processedMessages = store.getFolder("Processed");
		inbox.copyMessages(messages, processedMessages);

		for (int i = 0; i < messages.length; i++) {
			messages[i].setFlag(Flags.Flag.DELETED, true);
		}

		inbox.expunge();

		inbox.close(true);
		store.close();
	}

	private SelectedTeamValidation scanEmailPartsAndValidate(BodyPart part, ZonedDateTime receivedDate, String from)
			throws Exception {

		SelectedTeamValidation validationResult = null;

		Object content = part.getContent();

		if (content instanceof InputStream || content instanceof String) {

			if (part.isMimeType("text/plain")) {
				validationResult = handleTextEmailContent(content.toString().trim(), from);
			} else if (part.isMimeType("text/html")) {
				validationResult = handleHtmlEmailContent(content.toString(), from);
			}

			if (validationResult == null) {
				if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
						|| Part.INLINE.equalsIgnoreCase(part.getDisposition())
						|| (part.getFileName() != null && !part.getFileName().isEmpty())) {
					String attachementName = part.getFileName();
					loggerUtils.log("info", "Attachement found, name={}", attachementName);
					if (attachementName.equalsIgnoreCase("selections.txt")) {
						loggerUtils.log("info", "Message from {}, has selection attachment", from);
						selectionsFileAttached = true;
						validationResult = handleSelectionFile(part.getInputStream());
						validationResult.setFrom(from);
						validationResults.add(validationResult);
						loggerUtils.log("info", "Message from {} handled with ... SUCCESS!", from);
					} else if (attachementName.equalsIgnoreCase("WINMAIL.DAT")
							|| attachementName.equalsIgnoreCase("ATT00001.DAT")) {
						loggerUtils.log("info", "Message from {}, is a TNEF message", from);
						validationResult = handleTNEFMessage(part.getInputStream(), from);
						validationResult.setFrom(from);
						validationResults.add(validationResult);
						loggerUtils.log("info", "Message from {} handled with ... SUCCESS!", from);
					}
				}
			}
		}

		if (validationResult == null) {
			if (content instanceof Multipart) {
				Multipart multipart = (Multipart) content;
				for (int i = 0; i < multipart.getCount(); i++) {
					BodyPart bodyPart = multipart.getBodyPart(i);
					validationResult = scanEmailPartsAndValidate(bodyPart, receivedDate, from);
					if (validationResult != null) {
						break;
					}
				}
			}
		}

		return validationResult;
	}

	private SelectedTeamValidation handleTextEmailContent(String text, String from) {
		SelectedTeamValidation validationResult = null;

		if (text.indexOf("[team]") == 0 && text.indexOf("[end]") != -1) {
			text = text.substring(0, text.indexOf("[end]"));
			String[] lines = text.split("\\R+");

			loggerUtils.log("info", "Message from {}, has selection in text body", from);
			selectionsFileAttached = true;
			validationResult = handleSelectionEmailText(lines, "noid");
			validationResult.setFrom(from);
			validationResults.add(validationResult);
		} else if (text.indexOf("[start id=") != -1 && text.indexOf("[end]") != -1) {
			text = text.substring(text.indexOf("[start id="), text.indexOf("[end]"));
			String[] lines = text.split("\\R+");

			String idLine = lines[0];
			String id = idLine.split("=")[1].trim().replaceAll("]", "");

			loggerUtils.log("info", "Message from {}, has selection in text body", from);
			selectionsFileAttached = true;
			validationResult = handleSelectionEmailText(lines, id);
			validationResult.setFrom(from);
			validationResults.add(validationResult);
		}

		return validationResult;
	}

	private SelectedTeamValidation handleHtmlEmailContent(String content, String from) {
		SelectedTeamValidation validationResult = null;

		Document document = Jsoup.parse(content);
		document.outputSettings(new Document.OutputSettings().prettyPrint(false));// makes html() preserve
																					// linebreaks and spacing
		document.select("br").append("\\n");
		document.select("p").prepend("\\n\\n");
		String s = document.body().html().replaceAll("\\\\n", "\n");
		String text = Jsoup.clean(s, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false))
				.trim();

		if (text.indexOf("[team]") == 0 && text.indexOf("[end]") != -1) {
			text = text.substring(0, text.indexOf("[end]"));
			String[] lines = text.split("\\R+");

			loggerUtils.log("info", "Message from {}, has selection in html body", from);
			selectionsFileAttached = true;
			validationResult = handleSelectionEmailText(lines, "noid");
			validationResult.setFrom(from);
			validationResults.add(validationResult);
		} else if (text.indexOf("[start id=") != -1 && text.indexOf("[end]") != -1) {
			text = text.substring(text.indexOf("[start id="), text.indexOf("[end]"));
			String[] lines = text.split("\\R+");

			String idLine = lines[0];
			String id = idLine.split("=")[1].trim().replaceAll("]", "");

			loggerUtils.log("info", "Message from {}, has selection in text body", from);
			selectionsFileAttached = true;
			validationResult = handleSelectionEmailText(lines, id);
			validationResult.setFrom(from);
			validationResults.add(validationResult);
		}

		return validationResult;
	}

	private SelectedTeamValidation handleTNEFMessage(InputStream inputStream, String from)
			throws Exception {

		SelectedTeamValidation validationResult = null;

		TNEFInputStream tnefInputSteam = new TNEFInputStream(inputStream);
		net.freeutils.tnef.Message message = new net.freeutils.tnef.Message(tnefInputSteam);

		for (Attachment attachment : message.getAttachments()) {
			if (attachment.getNestedMessage() == null) {
				String filename = attachment.getFilename();

				if (filename.equals("selections.txt")) {
					loggerUtils.log("info", "Message from {}, has selection attachment", from);
					validationResult = handleSelectionFile(attachment.getRawData());
				}
			}
		}

		message.close();

		return validationResult;
	}

	private SelectedTeamValidation handleSelectionFile(InputStream inputStream)
			throws Exception {

		String line = "";
		String teamCode = "";
		int round = 0;
		List<Integer> ins = new ArrayList<>();
		List<Integer> outs = new ArrayList<>();
		List<Double> emgs = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

		loggerUtils.log("info", "Moving messages to Processed folder");

		while ((line = reader.readLine()) != null) {

			if (line.toLowerCase().contains("[team]")) {
				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.toLowerCase().contains("[round]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						teamCode = line;
					}
				}
				loggerUtils.log("info", "Selections for team: {}", teamCode);
			}

			if (line.toLowerCase().contains("[round]")) {
				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.toLowerCase().contains("[in]") || line.toLowerCase().contains("[out]")
							|| line.toLowerCase().contains("[emg]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						round = Integer.parseInt(line);
					}
				}
				loggerUtils.log("info", "Selections for round: {}", round);
			}

			if (line.toLowerCase().contains("[in]")) {
				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.toLowerCase().contains("[out]")) {
						break;
					} else if (line.toLowerCase().contains("[emg]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						ins.add(Integer.parseInt(line));
					}
				}
				loggerUtils.log("info", "Selection in: {}", ins);
			}

			if (line.toLowerCase().contains("[out]")) {
				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.toLowerCase().contains("[in]")) {
						break;
					} else if (line.toLowerCase().contains("[emg]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						outs.add(Integer.parseInt(line));
					}
				}
				loggerUtils.log("info", "Selection out: {}", outs);
			}

			if (line.toLowerCase().contains("[emg]")) {
				int emgCount = 1;
				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.toLowerCase().contains("[in]")) {
						break;
					} else if (line.toLowerCase().contains("[out]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						double emg = Double.parseDouble(line);
						if (emgCount == 1) {
							emg = emg + 0.1;
							emgCount++;
						} else {
							emg = emg + 0.2;
						}
						emgs.add(emg);
					}
				}
				loggerUtils.log("info", "Selection emergencies: {}", emgs);
			}
		}

		Map<String, List<Integer>> insAndOuts = new HashMap<>();
		insAndOuts.put("in", ins);
		insAndOuts.put("out", outs);

		SelectedTeamValidationHandler validationHandler = new SelectedTeamValidationHandler();
		validationHandler.configureLogging(mdcKey, loggerName, logfile);
		return validationHandler.execute(round, teamCode, insAndOuts, emgs, "noid");
	}

	private SelectedTeamValidation handleSelectionEmailText(String[] emailLines, String id) {

		String line = "";
		String teamCode = "";
		int round = 0;
		List<Integer> ins = new ArrayList<>();
		List<Integer> outs = new ArrayList<>();
		List<Double> emgs = new ArrayList<>();

		loggerUtils.log("info", "Moving messages to Processed folder");

		for (int i = 0; i < emailLines.length; i++) {

			line = emailLines[i];

			if (line.toLowerCase().contains("[team]")) {
				while (i < emailLines.length) {
					line = emailLines[i++].trim();
					if (line.toLowerCase().contains("[round]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						teamCode = line;
					}
				}
				loggerUtils.log("info", "Selections for team: {}", teamCode);
			}

			if (line.toLowerCase().contains("[round]")) {
				while (i < emailLines.length) {
					line = emailLines[i++].trim();
					if (line.toLowerCase().contains("[in]") || line.toLowerCase().contains("[out]")
							|| line.toLowerCase().contains("[emg]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						round = Integer.parseInt(line);
					}
				}
				loggerUtils.log("info", "Selections for round: {}", round);
			}

			if (line.toLowerCase().contains("[in]")) {
				while (i < emailLines.length) {
					line = emailLines[i++].trim();
					if (line.toLowerCase().contains("[out]")) {
						break;
					} else if (line.toLowerCase().contains("[emg]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						int in = getPlayerNo(line);
						if (in > 0) {
							ins.add(in);
						} else {
							loggerUtils.log("info", "Couldn't get player number for INs, No.={}", in);
						}
					}
				}
				loggerUtils.log("info", "Selection in: {}", ins);
			}

			if (line.toLowerCase().contains("[out]")) {
				while (i < emailLines.length) {
					line = emailLines[i++].trim();
					if (line.toLowerCase().contains("[in]")) {
						break;
					} else if (line.toLowerCase().contains("[emg]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						int out = getPlayerNo(line);
						if (out > 0) {
							outs.add(out);
						} else {
							loggerUtils.log("info", "Couldn't get player number for OUTs, No.={}", out);
						}
					}
				}
				loggerUtils.log("info", "Selection out: {}", outs);
			}

			if (line.toLowerCase().contains("[emg]")) {
				int emgCount = 1;
				while (i < emailLines.length) {
					line = emailLines[i++].trim();
					if (line.toLowerCase().contains("[in]")) {
						break;
					} else if (line.toLowerCase().contains("[out]")) {
						break;
					} else if (line.equalsIgnoreCase("")) {
						// ignore blank lines
					} else {
						double emg = getPlayerNo(line);
						if (emg > 0) {
							if (emgCount == 1) {
								emg = emg + 0.1;
								emgCount++;
							} else {
								emg = emg + 0.2;
							}
							emgs.add(emg);
						} else {
							loggerUtils.log("info", "Couldn't get player number for OUTs, No.={}", emg);
						}
					}
				}
				loggerUtils.log("info", "Selection emergencies: {}", emgs);
			}
		}

		boolean idHandledThisBatch = false;

		if (selectionsIdsCurrent.containsKey(id)) {
			String team = selectionsIdsCurrent.get(id);
			if (team.equalsIgnoreCase(teamCode)) {
				idHandledThisBatch = true;
			} else {
				selectionsIdsCurrent.put(id, teamCode);
			}
		} else {
			selectionsIdsCurrent.put(id, teamCode);
		}

		SelectedTeamValidation validationResult = null;

		if (idHandledThisBatch) {
			validationResult = new SelectedTeamValidation();
			validationResult.selectionFileMissing = false;
			validationResult.roundCompleted = false;
			validationResult.lockedOut = false;
			validationResult.duplicateSubmissionId = true;
			loggerUtils.log("info", "Already handled a selection in this batch for round={}. teamCode={}, id={}", round,
					teamCode, id);
		} else {
			Map<String, List<Integer>> insAndOuts = new HashMap<>();
			insAndOuts.put("in", ins);
			insAndOuts.put("out", outs);

			SelectedTeamValidationHandler validationHandler = new SelectedTeamValidationHandler();
			validationHandler.configureLogging(mdcKey, loggerName, logfile);
			validationResult = validationHandler.execute(round, teamCode, insAndOuts, emgs, id);
		}

		return validationResult;
	}

	private int getPlayerNo(String line) {

		int playerNo;
		String playerNoStr;

		Pattern pattern = Pattern.compile("[\\s:\\-\\.\\W]");
		Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			int i = matcher.start();
			playerNoStr = line.substring(0, i);
		} else {
			playerNoStr = line;
		}

		try {
			playerNo = Integer.parseInt(playerNoStr.trim());
		} catch (NumberFormatException e) {
			loggerUtils.log("info", "Error parsing player number, number format exception ... oh well.  Error={}",
					e.getMessage());
			playerNo = 0;
		}

		return playerNo;
	}

	private void sendResponses() throws Exception {
		for (SelectedTeamValidation validationResult : validationResults) {

			String to = "";

			if (!System.getenv("ENV").equals("production")) {
				to = this.emailOveride;
			} else {
				to = validationResult.getFrom();
			}

			String teamCode = validationResult.getTeamCode();

			Properties properties = new Properties();
			// Setup mail server
			properties.setProperty("mail.smtp.host", outgoingMailHost);
			properties.setProperty("mail.smtp.port", String.valueOf(outgoingMailPort));
			properties.setProperty("mail.smtp.starttls.enable", "true");
			properties.setProperty("mail.smtp.auth", "true");

			Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(mailUsername, mailPassword);
				}
			});

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(this.dflmngrEmailAddr));
			message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

			loggerUtils.log("info", "Creating response message: to={}; from={};", to, this.dflmngrEmailAddr);

			if (validationResult.isValid()) {
				if (teamCode != null && !teamCode.equals("")) {
					DflTeam team = dflTeamService.get(teamCode);
					String teamTo = team.getCoachEmail();
					loggerUtils.log("info", "Team email: {}", teamTo);
					if (!to.toLowerCase().contains(teamTo.toLowerCase())) {
						loggerUtils.log("info", "Adding team email");
						message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(teamTo));
					}
				}
				loggerUtils.log("info", "Message is for SUCCESS");
				setSuccessMessage(message, validationResult);
			} else {
				loggerUtils.log("info", "Message is for FAILURE");
				setFailureMessage(message, validationResult);
			}

			loggerUtils.log("info", "Sending message");

			Transport.send(message, message.getAllRecipients());
		}
	}

	private void setSuccessMessage(Message message, SelectedTeamValidation validationResult) throws Exception {
		message.setSubject("Selections received - SUCCESS!");

		String messageBody = "Coach, \n\n" + "Your selections have been stored in the database ....\n";

		if (validationResult.areWarnings()) {
			messageBody = messageBody + "\n";

			if (validationResult.selectedWarning) {
				messageBody = messageBody
						+ "\tWarning: You have seleted a player who is already selected.  You may be playing short! Players:\n";
				for (DflPlayer player : validationResult.selectedWarnPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.droppedWarning) {
				messageBody = messageBody
						+ "\tWarning: You have dropped a player who is not selected.  Your team may not be as you expect or invalid! Players:\n";
				for (DflPlayer player : validationResult.droppedWarnPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}

			if (validationResult.emergencyFfWarning) {
				messageBody = messageBody
						+ "\tWarning: You have selcted a Full Forward as an emergency but already have one on your bench.  It will be ignored.  Emgergency:\n";
				for (DflPlayer player : validationResult.emgFfPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.emergencyFwdWarning) {
				messageBody = messageBody
						+ "\tWarning: You have selcted a Forward as an emergency but already have one on your bench.  It will be ignored.  Emgergency:\n";
				for (DflPlayer player : validationResult.emgFwdPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.emergencyRckWarning) {
				messageBody = messageBody
						+ "\tWarning: You have selcted a Ruck as an emergency but already have one on your bench.  It will be ignored.  Emgergency:\n";
				for (DflPlayer player : validationResult.emgRckPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.emergencyMidWarning) {
				messageBody = messageBody
						+ "\tWarning: You have selcted a Midfielder as an emergency but already have one on your bench.  It will be ignored.  Emgergency:\n";
				for (DflPlayer player : validationResult.emgMidPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.emergencyDefWarning) {
				messageBody = messageBody
						+ "\tWarning: You have selcted a Defender as an emergency but already have one on your bench.  It will be ignored.  Emgergency:\n";
				for (DflPlayer player : validationResult.emgDefPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.emergencyFbWarning) {
				messageBody = messageBody
						+ "\tWarning: You have selcted a Full Back as an emergency but already have one on your bench.  It will be ignored.  Emgergency:\n";
				for (DflPlayer player : validationResult.emgFbPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.duplicateIns) {
				messageBody = messageBody + "\tWarning: You have selected duplicate ins, one will be ignored.  Ins:\n";
				for (DflPlayer player : validationResult.dupInPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.duplicateOuts) {
				messageBody = messageBody + "\tWarning: You have selected duplicate outs, one will be ignored.  Ins:\n";
				for (DflPlayer player : validationResult.dupInPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
			if (validationResult.duplicateEmgs) {
				messageBody = messageBody
						+ "\tWarning: You have selected duplicate emergencies, one will be ignored.  Ins:\n";
				for (DflPlayer player : validationResult.dupInPlayers) {

					DflTeamPlayer teamPlayer = dflTeamPlayerService.get(player.getPlayerId());

					messageBody = messageBody + "\t\t" + teamPlayer.getTeamPlayerId() + " " + player.getFirstName()
							+ " " + player.getLastName() + " " + player.getPosition() + " " + player.getPosition()
							+ "\n";
				}
			}
		}

		messageBody = messageBody + "\n\nHave a nice day. \n\n" + "DFL Manager Admin";

		message.setContent(messageBody, "text/plain");
	}

	private void setFailureMessage(Message message, SelectedTeamValidation validationResult) throws Exception {
		message.setSubject("Selections received - FAILED!");

		String messageBody = "Coach,\n\n"
				+ "Your selections have not been stored in the database .... The reasons for this are:\n";

		if (validationResult.playedSelections) {
			messageBody = messageBody
					+ "\t- You have selected/dropped a player who has already played and was not included in your previous selections.\n";
		} else if (validationResult.selectionFileMissing) {
			messageBody = messageBody
					+ "\t- You sent the email with no selections.txt or the selections were missing from the emil body\n";
		} else if (validationResult.roundCompleted) {
			messageBody = messageBody + "\t- The round you have in your selections.txt has past\n";
		} else if (validationResult.lockedOut) {
			messageBody = messageBody + "\t- The round you have in your selections as had all AFL games completed.\n";
		} else if (validationResult.unknownError) {
			messageBody = messageBody + "\t- Some exception occured follow up with email to xdfl google group.\n";
		} else if (validationResult.duplicateSubmissionId) {
			messageBody = messageBody + "\t- Already processed selection for your selection id.\n";
		} else if (!validationResult.teamPlayerCheckOk) {
			messageBody = messageBody + "\t- The ins and/or outs numbers sent are not correct\n";
		} else {
			if (!validationResult.ffCheckOk) {
				messageBody = messageBody + "\t- You have too many Full Forwards\n";
			}
			if (!validationResult.fwdCheckOk) {
				messageBody = messageBody + "\t- You have too many Forwards\n";
			}
			if (!validationResult.rckCheckOk) {
				messageBody = messageBody + "\t- You have too many Rucks\n";
			}
			if (!validationResult.midCheckOk) {
				messageBody = messageBody + "\t- You have too many Midfielders\n";
			}
			if (!validationResult.fbCheckOk) {
				messageBody = messageBody + "\t- You have too many Full Backs\n";
			}
			if (!validationResult.defCheckOk) {
				messageBody = messageBody + "\t- You have too many Defenders\n";
			}
			if (!validationResult.benchCheckOk) {
				messageBody = messageBody + "\t- You have too many on the bench.\n";
			}
		}

		messageBody = messageBody + "\nPlease check your selections.txt file and try again.  "
				+ "If it fails again, send an email to the google group and maybe if you are lucky someone will sort it out.\n\n"
				+ "DFL Manager Admin";

		message.setContent(messageBody, "text/plain");
	}

	// internal testing
	public static void main(String[] args) {
		try {
			EmailSelectionsHandler selectionHandler = new EmailSelectionsHandler();
			selectionHandler.execute();
			System.exit(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
