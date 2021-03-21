package net.dflmngr.utils;

import java.util.List;
//import java.util.Properties;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
//import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.utils.oauth2.AccessTokenFromRefreshToken;
import net.dflmngr.utils.oauth2.OAuth2Authenticator;

public class EmailUtils {

	private static GlobalsService globalsService = new GlobalsServiceImpl();
	// private static String incomingMailHost =
	// globalsService.getEmailConfig().get("incomingMailHost");
	private static String outgoingMailHost = globalsService.getEmailConfig().get("outgoingMailHost");
	private static int outgoingMailPort = Integer.parseInt(globalsService.getEmailConfig().get("outgoingMailPort"));
	// private static String mailUsername =
	// globalsService.getEmailConfig().get("mailUsername");;
	private static String mailPassword = globalsService.getEmailConfig().get("mailPassword");;

	private static String mailUsername;
	private static String emailOveride;

	static {
		mailUsername = globalsService.getEmailConfig().get("mailUsername");
		if (!System.getenv("ENV").equals("production")) {
			mailUsername = System.getenv("DFL_MNGR_EMAIL");
			emailOveride = System.getenv("EMAIL_OVERIDE");
		}

		OAuth2Authenticator.initialize();
	}

	public static void sendTextEmail(List<String> to, String from, String subject, String body,
			List<String> attachments) throws Exception {

		// MimeMessage message = new MimeMessage(getMailSession());
		Session session = getMailSession();
		MimeMessage message = new MimeMessage(session);
		// message.setFrom(new InternetAddress(from));

		// InternetAddress[] toAddresses = new InternetAddress[to.size()];
		InternetAddress[] toAddresses = null;

		// for(int i = 0; i < to.size(); i++) {
		// toAddresses[i] = new InternetAddress(to.get(i));
		// }

		if (!System.getenv("ENV").equals("production")) {
			message.setFrom(new InternetAddress(mailUsername));
			toAddresses = new InternetAddress[1];
			toAddresses[0] = new InternetAddress(emailOveride);
		} else {
			message.setFrom(new InternetAddress(from));
			toAddresses = new InternetAddress[to.size()];
			for (int i = 0; i < to.size(); i++) {
				toAddresses[i] = new InternetAddress(to.get(i));
			}
		}

		message.setRecipients(Message.RecipientType.TO, toAddresses);
		message.setSubject(subject);

		if (attachments != null && !attachments.isEmpty()) {
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body);

			Multipart multipart = new MimeMultipart("mixed");
			multipart.addBodyPart(messageBodyPart);

			for (String attachment : attachments) {
				messageBodyPart = new MimeBodyPart();

				DataSource source = new FileDataSource(attachment);
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(source.getName());
				multipart.addBodyPart(messageBodyPart);
			}

			message.setContent(multipart);
		} else {
			message.setContent(body, "text/plain");
		}

		Transport.send(message);
		// String oauthToken = AccessTokenFromRefreshToken.getAccessToken();
		// Transport smptTransport = OAuth2Authenticator.connectToSmtp(outgoingMailHost,
		// outgoingMailPort, mailUsername,
		// oauthToken, false);
		// smptTransport.sendMessage(message, message.getAllRecipients());
	}

	public static void sendHtmlEmail(List<String> to, String from, String subject, String body,
			List<String> attachments) throws Exception {

		// OAuth2Authenticator.initialize();

		// MimeMessage message = new MimeMessage(getMailSession());
		Session session = getMailSession();
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));

		InternetAddress[] toAddresses = new InternetAddress[to.size()];

		for (int i = 0; i < to.size(); i++) {
			toAddresses[i] = new InternetAddress(to.get(i));
		}

		message.setRecipients(Message.RecipientType.TO, toAddresses);
		message.setSubject(subject);

		if (attachments != null && !attachments.isEmpty()) {
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(body, "text/html");

			Multipart multipart = new MimeMultipart("mixed");
			multipart.addBodyPart(messageBodyPart);

			for (String attachment : attachments) {
				messageBodyPart = new MimeBodyPart();

				DataSource source = new FileDataSource(attachment);
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(source.getName());
				multipart.addBodyPart(messageBodyPart);
			}

			message.setContent(multipart);
		} else {
			message.setContent(body, "text/html");
		}

		Transport.send(message);
		// String oauthToken = AccessTokenFromRefreshToken.getAccessToken();
		// Transport smptTransport = OAuth2Authenticator.connectToSmtp(outgoingMailHost,
		// outgoingMailPort, mailUsername,
		// oauthToken, false);
		// smptTransport.sendMessage(message, message.getAllRecipients());
	}

	private static Session getMailSession() {

		Properties properties = new Properties();
		// Setup mail server
		properties.setProperty("mail.smtp.host", outgoingMailHost);
		properties.setProperty("mail.smtp.port", String.valueOf(outgoingMailPort));
		properties.setProperty("mail.smtp.starttls.enable", "true");
		// properties.setProperty("mail.smtp.ssl.enable", "true");
		properties.setProperty("mail.smtp.auth", "true");

		Session mailSession = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailUsername, mailPassword);
			}
		});

		return mailSession;
	}

}
