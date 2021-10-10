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
	private static final String EMAIL_BODY_RESOURCE = "EmailBodyText.txt";
	private static final String EMAIL_SUBJECT = "Missing VASO Student Permissions";

	private final Properties props;
	private final String emailBody;

	public Emailer() throws IOException {
		props = Util.loadPropertiesFromResource(Util.PROPERTIES_RESOURCE);
		emailBody = Util.getResourceAsString(EMAIL_BODY_RESOURCE);
	}

	public void send(File attachment, List<String> recipients, long numSStudentsNotFoundInP) {
		if (recipients == null || recipients.isEmpty()) {
			return;
		}
		try {
			Session session = Session.getDefaultInstance(props);

			BodyPart contentPart = new MimeBodyPart();
			//contentPart.setContent("<h1>This is some <i>HTML</i> text.</h1>", "text/html");
			contentPart.setContent(String.format(emailBody, numSStudentsNotFoundInP), "text/plain");

			BodyPart attachmentPart = new MimeBodyPart();
			attachmentPart.setFileName(attachment.getName());
			attachmentPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));

			Multipart multipartContent = new MimeMultipart();
			multipartContent.addBodyPart(contentPart);
			multipartContent.addBodyPart(attachmentPart);

			MimeMessage message = new MimeMessage(session);
			recipients.stream()
				.forEach(recipient -> addRecipient(message, recipient));
			message.setFrom(new InternetAddress(props.getProperty("mail.from")));
			message.setSubject(EMAIL_SUBJECT);
			message.setContent(multipartContent);

			Transport.send(message,
				props.getProperty("mail.user"),
				props.getProperty("mail.password"));
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
