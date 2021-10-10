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
import java.io.Writer;
import java.lang.reflect.Type;
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class PortalRetriever {
	private static class ReportResponse {
		@SuppressWarnings("unused")
		public ReportResponse() {
			total_pages = -1;
			current_page = -1;
			total_records = -1;
			records = null;
		}

		public ReportResponse(List<PortalStudent> students) {
			total_pages = 1;
			current_page = 1;
			total_records = students.size();
			records = students;
		}

		public int total_pages;
		public int current_page;
		@SuppressWarnings("unused")
		public int total_records;
		public List<PortalStudent> records;
	}

	private static class PortalStudentHandler
			implements JsonDeserializer<PortalStudent>, JsonSerializer<PortalStudent> {
		@Override
		public PortalStudent deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			String firstName = json.getAsJsonObject()
				.get("field_52").getAsJsonObject()
				.get("first").getAsString();
			String lastName = json.getAsJsonObject()
				.get("field_52").getAsJsonObject()
				.get("last").getAsString();
			String nickName = json.getAsJsonObject()
				.get("field_70").getAsString();
			String school = json.getAsJsonObject()
				.get("field_56").getAsJsonArray()
				.get(0).getAsJsonObject()
				.get("identifier").getAsString();
			int grade = json.getAsJsonObject()
				.get("field_90").getAsInt();
			return new PortalStudent(firstName, lastName, nickName, school, grade);
		}

		@Override
		public JsonElement serialize(PortalStudent src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject name = new JsonObject();
			name.add("first", new JsonPrimitive(src.firstName));
			name.add("last", new JsonPrimitive(src.lastName));

			JsonObject school = new JsonObject();
			school.add("id", new JsonPrimitive("unknown"));
			school.add("identifier", new JsonPrimitive(src.school));

			JsonArray schoolArray = new JsonArray();
			schoolArray.add(school);

			JsonObject result = new JsonObject();
			result.add("id", new JsonPrimitive("unknown"));
			result.add("field_52", name);
			result.add("field_70", new JsonPrimitive(src.nickName));
			result.add("field_56", schoolArray);
			result.add("field_90", new JsonPrimitive(src.grade));
			return result;
		}
	}

	private static final String ROSTER_FILE_FMT = "portal-%1$tFT%1$tT.json";
	private static final Pattern ROSTER_FILE_PATTERN = Pattern.compile("portal-.*\\.((json)|(csv))");
	private static final String JSON_MEDIA_TYPE = "application/json";
	private static final String TOKEN_URL = "https://api.knack.com/v1/applications/%1$s/session";
	private static final String TOKEN_BODY = "{\"email\":\"%1$s\",\"password\":\"%2$s\"}";
	private static final String REPORT_URL = "https://api.knack.com/v1/pages"
		+ "/scene_%1$s/views/view_%2$s/records?format=raw&sort_field=%3$s"
		+ "&sort_order=asc&page=%4$d&rows_per_page=%5$d";
	private static final int PAGE_SIZE = 100;

	private final File rosterDir;
	private final String user;
	private final String password;
	private final String applicationId;
	private final String scene;
	private final String view;
	private final String sortField;
	private final HttpClient client;
	private String userToken;
	private int totalPages;
	private int lastPageRead;	// 1-based
	private List<PortalStudent> students;

	public PortalRetriever() {
		Properties props = Util.loadPropertiesFromResource(Util.PROPERTIES_RESOURCE);
		rosterDir = new File(props.getProperty("portal.roster.dir"));
		user = props.getProperty("portal.user");
		password = props.getProperty("portal.password");
		applicationId = props.getProperty("portal.application.id");
		scene = props.getProperty("portal.scene");
		view = props.getProperty("portal.view");
		sortField = props.getProperty("portal.sort.field");

		client = HttpClient.newHttpClient();
		userToken = null;
		totalPages = -1;
		lastPageRead = -1;
		students = new ArrayList<>();
	}

	public void saveRoster() throws IOException {
		retrieveRoster();
		File rosterFile = new File(rosterDir,
			String.format(ROSTER_FILE_FMT, LocalDateTime.now()));
		if (!rosterDir.exists()) {
			rosterDir.mkdirs();
		}
		try (
			OutputStream os = new FileOutputStream(rosterFile);
			Writer wtr = new OutputStreamWriter(os, StandardCharsets.UTF_8);
		) {
			Gson gson = new GsonBuilder()
				.registerTypeAdapter(PortalStudent.class, new PortalStudentHandler())
				.create();
			gson.toJson(new ReportResponse(students), ReportResponse.class, wtr);
		}
	}

	private void retrieveRoster() throws IOException {
		getUserToken();
		System.out.format("Found usr token '%1$s'%n", userToken);
		for (int currentPage = 1;; ++currentPage) {
			String url = String.format(REPORT_URL, scene, view, sortField, currentPage, PAGE_SIZE);
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.GET()
				.header("Accept", JSON_MEDIA_TYPE)
				.header("X-Knack-Application-Id", applicationId)
				.header("Authorization", userToken)
				.build();
			client.sendAsync(request, BodyHandlers.ofInputStream())
				.thenApply(HttpResponse::body)
				.thenAccept(this::readJsonReport)
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

	private void readJsonReport(InputStream is) {
		Reader rdr = new InputStreamReader(is, StandardCharsets.UTF_8);
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(PortalStudent.class, new PortalStudentHandler())
			.create();
		ReportResponse response = gson.fromJson(rdr, new TypeToken<ReportResponse>(){}.getType());
		totalPages = response.total_pages;
		lastPageRead = response.current_page;
		students.addAll(response.records);
	}

	public List<PortalStudent> readLatestRosterFile() throws IOException {
		File rosterFile;
		try (Stream<Path> stream = Files.find(rosterDir.toPath(), Integer.MAX_VALUE,
			PortalRetriever::matcher, FileVisitOption.FOLLOW_LINKS)) {
			rosterFile = stream
				.max(Comparator.comparing(path -> path.getFileName().toString()))
				.map(Path::toFile)
				.orElse(null);
		}

		if (rosterFile == null) {
			return List.of();
		}

		if ("csv".equalsIgnoreCase(Util.getExt(rosterFile))) {
			return PortalStudent.parse(rosterFile);
		}

		try (
			InputStream os = new FileInputStream(rosterFile);
			Reader rdr = new InputStreamReader(os, StandardCharsets.UTF_8);
		) {
			Gson gson = new GsonBuilder()
				.registerTypeAdapter(PortalStudent.class, new PortalStudentHandler())
				.create();
			ReportResponse response = gson.fromJson(rdr, ReportResponse.class);
			return response.records;
		}
	}

	private static boolean matcher(Path path, BasicFileAttributes attrs) {
		return attrs.isRegularFile()
			&& ROSTER_FILE_PATTERN.matcher(path.getFileName().toString()).matches();
	}

	public static void main(String [] args) {
		try {
			PortalRetriever retriever = new PortalRetriever();
			retriever.saveRoster();
			List<PortalStudent> students = retriever.readLatestRosterFile();
			System.out.format("Found %1$d students:%n", students.size());
			students.forEach(student -> System.out.format("   %1$s%n", student));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
