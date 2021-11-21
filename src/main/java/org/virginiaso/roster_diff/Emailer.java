package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Emailer {
	private static final String EMAIL_BODY_RESOURCE = "EmailBody.html";
	private static final String EMAIL_SUBJECT = "Missing VASO Student Permissions";
	private static final String MEDIA_TYPE = "text/html";

	private final Session session;
	private final String emailBody;
	private final String fromAddr;
	private final String userName;
	private final String password;

	public static void main(String[] args) {
		try {
			File attachment = new File("README.md");
			List<String> recipients = List.of(
				//"Karen Emmons <karen@emmons.mobi>",
				"Ian Emmons <ian@emmons.mobi>");
			Emailer emailer = new Emailer();
			emailer.send(attachment, recipients);
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	public Emailer() throws IOException {
		Properties props = Util.loadPropertiesFromResource(Util.PROPERTIES_RESOURCE);
		session = Session.getDefaultInstance(props);
		emailBody = Util.getResourceAsString(EMAIL_BODY_RESOURCE);
		fromAddr = props.getProperty("mail.from");
		userName = props.getProperty("mail.user");
		password = props.getProperty("mail.password");
	}

	public void send(File attachment, List<String> recipients) {
		if (recipients == null || recipients.isEmpty()) {
			return;
		}
		try {
			BodyPart contentPart = new MimeBodyPart();
			contentPart.setContent(emailBody, MEDIA_TYPE);

			BodyPart attachmentPart = new MimeBodyPart();
			attachmentPart.setFileName(attachment.getName());
			attachmentPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));

			Multipart multipartContent = new MimeMultipart();
			multipartContent.addBodyPart(contentPart);
			multipartContent.addBodyPart(attachmentPart);

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromAddr));
			recipients.stream()
				.forEach(recipient -> addRecipient(message, recipient));
			message.setSubject(EMAIL_SUBJECT);
			message.setContent(multipartContent);

			Transport.send(message, userName, password);
			System.out.println("Mail successfully sent");
		} catch (MessagingException ex) {
			throw new UncheckedMessagingException(ex);
		}
	}

	private static void addRecipient(MimeMessage message, String recipient) {
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
		} catch (MessagingException ex) {
			throw new UncheckedMessagingException(ex);
		}
	}
}
