package net.dflmngr.utils;

import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.dflmngr.exceptions.EmailException;
import net.dflmngr.model.service.GlobalsService;
import net.dflmngr.model.service.impl.GlobalsServiceImpl;
import net.dflmngr.utils.oauth2.OAuth2Authenticator;

public class EmailUtils {

	private static GlobalsService globalsService = new GlobalsServiceImpl();
	private static String outgoingMailHost = globalsService.getEmailConfig().get("outgoingMailHost");
	private static int outgoingMailPort = Integer.parseInt(globalsService.getEmailConfig().get("outgoingMailPort"));
	private static String mailPassword = globalsService.getEmailConfig().get("mailPassword");

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

	private EmailUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static void sendTextEmail(List<String> to, String from, String subject, String body, List<String> attachments) {

		Session session = getMailSession();
		MimeMessage message = new MimeMessage(session);
		InternetAddress[] toAddresses = null;

		try {
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
		} catch (MessagingException e) {
			throw new EmailException();
		}
	}

	public static void sendHtmlEmail(List<String> to, String from, String subject, String body, List<String> attachments) {

		Session session = getMailSession();
		MimeMessage message = new MimeMessage(session);

		
		try {
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
		} catch (MessagingException e) {
			throw new EmailException();
		}
	}

	private static Session getMailSession() {

		Properties properties = new Properties();
		// Setup mail server
		properties.setProperty("mail.smtp.host", outgoingMailHost);
		properties.setProperty("mail.smtp.port", String.valueOf(outgoingMailPort));
		properties.setProperty("mail.smtp.starttls.enable", "true");
		properties.setProperty("mail.smtp.auth", "true");

		return Session.getInstance(properties, new javax.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailUsername, mailPassword);
			}
		});
	}

}
