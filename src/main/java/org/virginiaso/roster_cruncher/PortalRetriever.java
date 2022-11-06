package org.virginiaso.roster_cruncher;

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

	private static final String REPORT_URL = "https://api.knack.com/v1/pages/scene_%1$s/"
		+ "views/view_%2$s/records?format=raw&page=%3$d&rows_per_page=%4$d";
	private static final int PAGE_SIZE = 100;

	// From the configuration file:
	private final File reportDir;

	// From the factory:
	private final Type reportResponseType;
	private final Gson gson;
	private final String reportName;
	private final String fileNameFormat;
	private final Pattern fileNamePattern;
	private final String scene;
	private final String view;

	// Computed here:
	private int totalPages;
	private int lastPageRead;	// 1-based
	private List<Item> reportItems;

	public PortalRetriever(Gson gson, String reportName, Type reportResponseType) {
		var props = Util.loadPropertiesFromResource(Util.CONFIGURATION_RESOURCE);
		var appName = props.getProperty("portal.application.name");
		reportDir = Util.parseFileArgument(props, "portal.report.dir");
		scene = props.getProperty("portal.%1$s.%2$s.scene".formatted(appName, reportName));
		view = props.getProperty("portal.%1$s.%2$s.view".formatted(appName, reportName));

		this.reportResponseType = reportResponseType;
		this.gson = gson;
		this.reportName = reportName;
		fileNameFormat = reportName + "-%1$tFT%1$tT.json";
		fileNamePattern = Pattern.compile(reportName + "-.*\\.json");

		totalPages = -1;
		lastPageRead = -1;
		reportItems = new ArrayList<>();
	}

	private static class StringHolder {
		public String string = null;
	}
	public void saveRawReport() throws IOException {
		var httpRequest = getHttpRequest(1);
		var stringHolder = new StringHolder();
		HttpClient.newHttpClient()
			.sendAsync(httpRequest, BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(body -> stringHolder.string = body)
			.join();

		var fileName = "raw-portal-%1$s-report-body.json".formatted(reportName);
		try (var pw = new PrintWriter(fileName, Util.CHARSET)) {
			pw.print(stringHolder.string);
		}
	}

	public void saveReport() throws IOException {
		retrieveReport();
		var reportFile = new File(reportDir, fileNameFormat.formatted(LocalDateTime.now()));
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
		for (int currentPage = 1;; ++currentPage) {
			var httpRequest = getHttpRequest(currentPage);
			HttpClient.newHttpClient()
				.sendAsync(httpRequest, BodyHandlers.ofInputStream())
				.thenApply(HttpResponse::body)
				.thenAccept(is -> reportItems.addAll(readJsonReport(is, this)))
				.join();
			if (lastPageRead >= totalPages) {
				break;
			}
		}
	}

	private HttpRequest getHttpRequest(int currentPage) {
		var url = REPORT_URL.formatted(scene, view, currentPage, PAGE_SIZE);
		return HttpRequest.newBuilder(URI.create(url))
			.GET()
			.header("Accept", Util.JSON_MEDIA_TYPE)
			.header("X-Knack-Application-Id", PortalUserToken.inst().getApplicationId())
			.header("Authorization", PortalUserToken.inst().getUserToken())
			.build();
	}

	public List<Item> readLatestReportFile() throws IOException {
		Stopwatch timer = new Stopwatch();
		File reportFile;
		try (Stream<Path> stream = Files.find(reportDir.toPath(), Integer.MAX_VALUE,
			this::matcher, FileVisitOption.FOLLOW_LINKS)) {
			reportFile = stream
				.map(Path::toFile)
				.max(Comparator.comparing(File::getName))
				.orElse(null);
		}

		if (reportFile == null) {
			return List.of();
		}

		try (InputStream is = new FileInputStream(reportFile)) {
			List<Item> items = readJsonReport(is, (PortalRetriever<Item>) null);
			timer.stopAndReport("Parsed Portal %1$s file".formatted(reportName));
			return items;
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
			PortalRetriever<Student> rosterRetriever = StudentRetrieverFactory.create();
			PortalRetriever<Coach> coachRetriever = CoachRetrieverFactory.create();

			//rosterRetriever.saveRawReport();
			//coachRetriever.saveRawReport();

			rosterRetriever.saveReport();
			List<Student> students = rosterRetriever.readLatestReportFile();
			System.out.format("Found %1$d students:%n", students.size());
			//students.forEach(student -> System.out.format("   %1$s%n", student));

			coachRetriever.saveReport();
			List<Coach> coaches = coachRetriever.readLatestReportFile();
			System.out.format("Found %1$d coaches:%n", coaches.size());
			//coaches.forEach(coach -> System.out.format("   %1$s%n", coach));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
