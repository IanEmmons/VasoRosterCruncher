package org.virginiaso.roster_diff;

import java.io.File;
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

public class SendEmail {
	public static void main(String [] args) {
		Properties props = Util.loadPropertiesFromResource(Util.PROPERTIES_RESOURCE);

		String recipient = props.getProperty("mail.from");
		String sender = props.getProperty("mail.from");
		File attachment = new File("src/main/resources/SchoolNameNormalizations.csv");

		Session session = Session.getDefaultInstance(props);

		try {
			BodyPart contentPart = new MimeBodyPart();
			//contentPart.setContent("<h1>This is some <i>HTML</i> text.</h1>", "text/html");
			contentPart.setContent("This is some plain text.", "text/plain");

			BodyPart attachmentPart = new MimeBodyPart();
			attachmentPart.setFileName(attachment.getName());
			attachmentPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));

			Multipart multipartContent = new MimeMultipart();
			multipartContent.addBodyPart(contentPart);
			multipartContent.addBodyPart(attachmentPart);

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(sender));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
			message.setSubject("Test Email Subject");
			message.setContent(multipartContent);

			Transport.send(message,
				props.getProperty("mail.user"),
				props.getProperty("mail.password"));
			System.out.println("Mail successfully sent");
		} catch (MessagingException ex) {
			ex.printStackTrace();
		}
	}
}
