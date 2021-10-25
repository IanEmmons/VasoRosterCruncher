package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class PortalRetriever<Item> {
	private static class ReportResponse<Item> {
		@SuppressWarnings("unused")
		public ReportResponse() {
			total_pages = -1;
			current_page = -1;
			total_records = -1;
			records = null;
		}

		public ReportResponse(List<Item> students) {
			total_pages = 1;
			current_page = 1;
			total_records = students.size();
			records = students;
		}

		public int total_pages;
		public int current_page;
		@SuppressWarnings("unused")
		public int total_records;
		public List<Item> records;
	}

	private static final String JSON_MEDIA_TYPE = "application/json";
	private static final String TOKEN_URL = "https://api.knack.com/v1/applications/%1$s/session";
	private static final String TOKEN_BODY = "{\"email\":\"%1$s\",\"password\":\"%2$s\"}";
	private static final String REPORT_URL = "https://api.knack.com/v1/pages/scene_%1$d/"
		+ "views/view_%2$d/records?format=raw&page=%3$d&rows_per_page=%4$d";
	private static final int PAGE_SIZE = 100;

	// From the configuration file:
	private final File reportDir;
	private final String user;
	private final String password;
	private final String applicationId;

	// From the factory:
	private final Gson gson;
	private final String fileNameFormat;
	private final Pattern fileNamePattern;
	private final int scene;
	private final int view;

	// Computed here:
	private final HttpClient client;
	private String userToken;
	private int totalPages;
	private int lastPageRead;	// 1-based
	private List<Item> reportItems;

	public PortalRetriever(Gson gson, String fileNamePrefix, int scene, int view) {
		Properties props = Util.loadPropertiesFromResource(Util.PROPERTIES_RESOURCE);
		reportDir = new File(props.getProperty("portal.roster.dir"));
		user = props.getProperty("portal.user");
		password = props.getProperty("portal.password");
		applicationId = props.getProperty("portal.application.id");

		this.gson = gson;
		fileNameFormat = fileNamePrefix + "-%1$tFT%1$tT.json";
		fileNamePattern = Pattern.compile(fileNamePrefix + "-.*\\.json");
		this.scene = scene;
		this.view = view;

		client = HttpClient.newHttpClient();
		userToken = null;
		totalPages = -1;
		lastPageRead = -1;
		reportItems = new ArrayList<>();
	}

	public void saveReport() throws IOException {
		retrieveReport();
		File reportFile = new File(reportDir, String.format(fileNameFormat, LocalDateTime.now()));
		if (!reportDir.exists()) {
			reportDir.mkdirs();
		}
		try (OutputStream os = new FileOutputStream(reportFile)) {
			writeJsonReport(os, reportItems);
		}
	}

	private void retrieveReport() throws IOException {
		getUserToken();
		System.out.format("Found usr token '%1$s'%n", userToken);
		for (int currentPage = 1;; ++currentPage) {
			String url = String.format(REPORT_URL, scene, view, currentPage, PAGE_SIZE);
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.GET()
				.header("Accept", JSON_MEDIA_TYPE)
				.header("X-Knack-Application-Id", applicationId)
				.header("Authorization", userToken)
				.build();
			client.sendAsync(request, BodyHandlers.ofInputStream())
				.thenApply(HttpResponse::body)
				.thenAccept(is -> reportItems.addAll(readJsonReport(is, this)))
				.join();
			if (lastPageRead >= totalPages) {
				break;
			}
		}
	}

	private void getUserToken() throws IOException {
		String url = String.format(TOKEN_URL, applicationId);
		String requestBody = String.format(TOKEN_BODY, user, password);
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
			.POST(BodyPublishers.ofString(requestBody))
			.header("Content-Type", JSON_MEDIA_TYPE)
			.build();
		client.sendAsync(request, BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(this::setUserToken)
			.join();
	}

	private void setUserToken(String jsonResponseBody) {
		userToken = JsonParser.parseString(jsonResponseBody).getAsJsonObject()
			.get("session").getAsJsonObject()
			.get("user").getAsJsonObject()
			.get("token").getAsString();
	}

	public List<Item> readLatestReportFile() throws IOException {
		File reportFile;
		try (Stream<Path> stream = Files.find(reportDir.toPath(), Integer.MAX_VALUE,
			this::matcher, FileVisitOption.FOLLOW_LINKS)) {
			reportFile = stream
				.max(Comparator.comparing(path -> path.getFileName().toString()))
				.map(Path::toFile)
				.orElse(null);
		}

		if (reportFile == null) {
			return List.of();
		}

		try (InputStream is = new FileInputStream(reportFile)) {
			return readJsonReport(is, null);
		}
	}

	private boolean matcher(Path path, BasicFileAttributes attrs) {
		String fName = path.getFileName().toString();
		return attrs.isRegularFile() && fileNamePattern.matcher(fName).matches();
	}

	private List<Item> readJsonReport(InputStream is, PortalRetriever<Item> retriever) {
		try (Reader rdr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			ReportResponse<Item> response = gson.fromJson(rdr,
				new TypeToken<ReportResponse<Item>>(){}.getType());
			if (retriever != null) {
				retriever.totalPages = response.total_pages;
				retriever.lastPageRead = response.current_page;
			}
			return response.records;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void writeJsonReport(OutputStream os, List<Item> students) {
		try (Writer wtr = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
			gson.toJson(new ReportResponse<Item>(students),
				new TypeToken<ReportResponse<Item>>(){}.getType(), wtr);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static void main(String [] args) {
		try {
			PortalRetriever<PortalStudent> rosterRetriever = PortalRosterRetrieverFactory.create();
			rosterRetriever.saveReport();
			List<PortalStudent> students = rosterRetriever.readLatestReportFile();
			System.out.format("Found %1$d students:%n", students.size());
			students.forEach(student -> System.out.format("   %1$s%n", student));

			PortalRetriever<Coach> coachRetriever = CoachRetrieverFactory.create();
			coachRetriever.saveReport();
			List<Coach> coaches = coachRetriever.readLatestReportFile();
			System.out.format("Found %1$d coaches:%n", coaches.size());
			coaches.forEach(coach -> System.out.format("   %1$s%n", coach));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
