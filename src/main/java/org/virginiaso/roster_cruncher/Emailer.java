package org.virginiaso.roster_cruncher;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class Emailer {
	private static final String MEDIA_TYPE = "text/html";

	private Emailer() {}	// prevents instantiation

	public static void main(String[] args) {
		try {
			List<String> recipients = List.of(
				//"Karen Emmons <karen@emmons.mobi>",
				"Ian Emmons <ian@emmons.mobi>");
			File attachment = new File("README.md");
			BiPredicate<Path, BasicFileAttributes> reportsDirPredicate = (path, attrs)
				-> attrs.isDirectory() && path.getFileName().toString().startsWith("reports-");
			BiPredicate<Path, BasicFileAttributes> reportFilePredicate = (path, attrs)
				-> attrs.isRegularFile() && path.getFileName().toString().endsWith(".html");
			find(Path.of("."), reportsDirPredicate)
				.max(Comparator.comparing(Path::toString))
				.flatMap(dir -> find(dir, reportFilePredicate).min(Comparator.comparing(Path::toString)))
				.map(Emailer::readFileContent)
				.ifPresent(reportBody -> Emailer.send("Test Email", reportBody, attachment, "Test School", recipients));
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

	public static void send(String emailSubject, String emailBody, File attachment,
			String schoolName, List<String> recipients) {
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

			MimeMessage message = new MimeMessage(Config.inst().getEmailSession());
			message.setFrom(new InternetAddress(Config.inst().getMailFromAddr()));
			recipients.stream()
				.forEach(recipient -> addRecipient(message, recipient));
			message.setSubject(emailSubject);
			message.setContent(multipartContent);

			Transport.send(message, Config.inst().getMailUserName(), Config.inst().getMailPassword());
			System.out.format("Email sent to %1$s (%2$s)%n",
				schoolName, recipients.stream().collect(Collectors.joining(", ")));
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
