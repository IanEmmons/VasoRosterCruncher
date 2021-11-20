package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

public class PortalRetriever<Item> {
	public static class ReportResponse<Item> {
		public ReportResponse() {
			total_pages = -1;
			current_page = -1;
			total_records = -1;
			records = null;
		}

		public ReportResponse(List<Item> items) {
			total_pages = 1;
			current_page = 1;
			total_records = items.size();
			records = items;
		}

		public int total_pages;
		public int current_page;
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
	private final Type reportResponseType;
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

	public PortalRetriever(Type reportResponseType, Gson gson, String fileNamePrefix, int scene, int view) {
		var props = Util.loadPropertiesFromResource(Util.PROPERTIES_RESOURCE);
		reportDir = new File(props.getProperty("portal.roster.dir"));
		user = props.getProperty("portal.user");
		password = props.getProperty("portal.password");
		applicationId = props.getProperty("portal.application.id");

		this.reportResponseType = reportResponseType;
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

	private static class StringHolder {
		public String string = null;
	}
	public void saveRawReport() throws IOException {
		getUserToken();
		System.out.format("Found usr token '%1$s'%n", userToken);

		var httpRequest = getHttpRequest(1);
		var stringHolder = new StringHolder();
		client.sendAsync(httpRequest, BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(body -> stringHolder.string = body)
			.join();

		try (var pw = new PrintWriter("raw-portal-report-body.json", Util.CHARSET)) {
			pw.print(stringHolder.string);
		}
	}

	public void saveReport() throws IOException {
		retrieveReport();
		var reportFile = new File(reportDir, String.format(fileNameFormat, LocalDateTime.now()));
		if (!reportDir.exists()) {
			reportDir.mkdirs();
		}
		try (
			OutputStream os = new FileOutputStream(reportFile);
			Writer wtr = new OutputStreamWriter(os, Util.CHARSET);
			JsonWriter jwtr = new JsonWriter(wtr);
		) {
			jwtr.setIndent("\t");
			gson.toJson(new ReportResponse<Item>(reportItems), reportResponseType, jwtr);
		}
	}

	private void retrieveReport() throws IOException {
		getUserToken();
		System.out.format("Found usr token '%1$s'%n", userToken);
		for (int currentPage = 1;; ++currentPage) {
			var httpRequest = getHttpRequest(currentPage);
			client.sendAsync(httpRequest, BodyHandlers.ofInputStream())
				.thenApply(HttpResponse::body)
				.thenAccept(is -> reportItems.addAll(readJsonReport(is, this)))
				.join();
			if (lastPageRead >= totalPages) {
				break;
			}
		}
	}

	private HttpRequest getHttpRequest(int currentPage) {
		var url = String.format(REPORT_URL, scene, view, currentPage, PAGE_SIZE);
		return HttpRequest.newBuilder(URI.create(url))
			.GET()
			.header("Accept", JSON_MEDIA_TYPE)
			.header("X-Knack-Application-Id", applicationId)
			.header("Authorization", userToken)
			.build();
	}

	private void getUserToken() throws IOException {
		if (userToken == null || userToken.isBlank()) {
			var url = String.format(TOKEN_URL, applicationId);
			var requestBody = String.format(TOKEN_BODY, user, password);
			var httpRequest = HttpRequest.newBuilder(URI.create(url))
				.POST(BodyPublishers.ofString(requestBody))
				.header("Content-Type", JSON_MEDIA_TYPE)
				.build();
			client.sendAsync(httpRequest, BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.thenAccept(this::setUserToken)
				.join();
		}
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
			return readJsonReport(is, (PortalRetriever<Item>) null);
		}
	}

	private boolean matcher(Path path, BasicFileAttributes attrs) {
		var fName = path.getFileName().toString();
		return attrs.isRegularFile() && fileNamePattern.matcher(fName).matches();
	}

	private List<Item> readJsonReport(InputStream is, PortalRetriever<Item> retriever) {
		try (Reader rdr = new InputStreamReader(is, Util.CHARSET)) {
			ReportResponse<Item> response = gson.fromJson(rdr, reportResponseType);
			if (retriever != null) {
				retriever.totalPages = response.total_pages;
				retriever.lastPageRead = response.current_page;
			}
			return response.records;
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
			//coachRetriever.saveRawReport();
			coachRetriever.saveReport();
			List<Coach> coaches = coachRetriever.readLatestReportFile();
			System.out.format("Found %1$d coaches:%n", coaches.size());
			coaches.forEach(coach -> System.out.format("   %1$s%n", coach));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
