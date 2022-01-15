package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private static final String MEDIA_TYPE = "text/html";

	private final Session session;
	private final String fromAddr;
	private final String userName;
	private final String password;

	public static void main(String[] args) {
		try {
			List<String> recipients = List.of(
				//"Karen Emmons <karen@emmons.mobi>",
				"Ian Emmons <ian@emmons.mobi>");
			File attachment = new File("README.md");
			Emailer emailer = new Emailer();
			BiPredicate<Path, BasicFileAttributes> reportsDirPredicate = (path, attrs)
				-> attrs.isDirectory() && path.getFileName().toString().startsWith("reports-");
			BiPredicate<Path, BasicFileAttributes> reportFilePredicate = (path, attrs)
				-> attrs.isRegularFile() && path.getFileName().toString().endsWith(".html");
			find(Path.of("."), reportsDirPredicate)
				.max(Comparator.comparing(Path::toString))
				.flatMap(dir -> find(dir, reportFilePredicate).min(Comparator.comparing(Path::toString)))
				.map(Emailer::readFileContent)
				.ifPresent(reportBody -> emailer.send("Test Email", reportBody, attachment, recipients));
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private static Stream<Path> find(Path start, BiPredicate<Path, BasicFileAttributes> matcher) {
		try {
			return Files.find(start, 1, matcher);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String readFileContent(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public Emailer() throws IOException {
		Properties props = Util.loadPropertiesFromResource(Util.CONFIGURATION_RESOURCE);
		session = Session.getDefaultInstance(props);
		fromAddr = props.getProperty("mail.from");
		userName = props.getProperty("mail.user");
		password = props.getProperty("mail.password");
	}

	public void send(String emailSubject, String emailBody, File attachment, List<String> recipients) {
		if (recipients == null || recipients.isEmpty()) {
			return;
		}
		try {
			Multipart multipartContent = new MimeMultipart();

			BodyPart contentPart = new MimeBodyPart();
			contentPart.setContent(emailBody, MEDIA_TYPE);
			multipartContent.addBodyPart(contentPart);

			if (attachment != null) {
				BodyPart attachmentPart = new MimeBodyPart();
				attachmentPart.setFileName(attachment.getName());
				attachmentPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));
				multipartContent.addBodyPart(attachmentPart);
			}

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromAddr));
			recipients.stream()
				.forEach(recipient -> addRecipient(message, recipient));
			message.setSubject(emailSubject);
			message.setContent(multipartContent);

			Transport.send(message, userName, password);
			System.out.format("Mail successfully sent to %1$s%n",
				recipients.stream().collect(Collectors.joining(", ")));
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
