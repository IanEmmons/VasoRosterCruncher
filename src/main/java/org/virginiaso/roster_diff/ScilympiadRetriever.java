package org.virginiaso.roster_diff;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

// https://scilympiad.com/va-div-a/Account/Login?email=ian%40emmons.mobi
public class ScilympiadRetriever {
	private static final String LOGIN_URL = "https://scilympiad.com/va-div-a/Account/Login";
	private static final String TOKEN_SELECTOR = "input[name=__RequestVerificationToken]";

	private final HttpClient client;
	private List<HttpCookie> cookies;
	private String verificationToken;
	private boolean loginSuccess;

	public ScilympiadRetriever() {
		client = HttpClient.newHttpClient();
		verificationToken = null;
		loginSuccess = false;
	}

	public void getReport() {
		getInitialRequestVerificationToken();
		System.out.format("Initial verification token: '%1$s'%n", verificationToken);
		logIn();
		if (loginSuccess) {
			System.out.format("Login succeeded%n");
		} else {
			System.out.format("Login failed%n");
		}
	}

	private void getInitialRequestVerificationToken() {
		HttpRequest request = HttpRequest.newBuilder(URI.create(LOGIN_URL))
			.GET()
			.header("cookie", cookieHeaderValue())
			.build();
		client.sendAsync(request, BodyHandlers.ofString())
			//.thenApply(headers -> printHeaders(headers, "Initial response headers:"))
			.thenApply(this::setCookies)
			.thenApply(HttpResponse::body)
			.thenAccept(this::setVerificationToken)
			.join();
	}

	private void setVerificationToken(String htmlResponseBody) {
		Document doc = Jsoup.parse(htmlResponseBody);
		verificationToken = doc.select(TOKEN_SELECTOR).stream()
			.findFirst()
			.map(tokenInputElement -> tokenInputElement.attr("value"))
			.orElse(null);
	}

	private void logIn() {
		Map<String, String> params = new HashMap<>();
		params.put("__RequestVerificationToken", verificationToken);
		params.put("Email", "statedirector@virginiaso.org");
		params.put("Password", "ynXVGhruXz7%83qK");
		params.put("RememberMe", "false");

		HttpRequest request = HttpRequest.newBuilder(URI.create(LOGIN_URL))
			.POST(ofFormData(params))
			.header("cookie", cookieHeaderValue())
			.build();
		client.sendAsync(request, BodyHandlers.ofString())
			//.thenApply(headers -> printHeaders(headers, "Login response headers:"))
			.thenApply(this::setCookies)
			.thenApply(HttpResponse::body)
			.thenAccept(this::checkLoginSuccess)
			.join();
	}

	public static HttpRequest.BodyPublisher ofFormData(Map<String, String> data) {
		return HttpRequest.BodyPublishers.ofString(data.entrySet().stream()
			.map(entry -> "%1$s=%2$s".formatted(
				URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
				URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)))
			.collect(Collectors.joining("&")));
	}

	private void checkLoginSuccess(String htmlResponseBody) {
		try (PrintWriter pw = new PrintWriter("page.html")) {
			pw.print(htmlResponseBody);
		} catch (FileNotFoundException ex) {
			throw new UncheckedIOException(ex);
		}
		Document doc = Jsoup.parse(htmlResponseBody);
		loginSuccess = doc.select("a[href=https://virginiaso.knack.com/vaso-portal-2022#coach-home/]").stream()
			.findFirst()
			.isPresent();
	}

	@SuppressWarnings("unused")
	private static HttpResponse<String> printHeaders(HttpResponse<String> response, String message) {
		System.out.format("%1$s%n", message);
		response.headers().map().entrySet().stream().forEach(entry -> {
			System.out.format("   '%1$s':%n", entry.getKey());
			entry.getValue().stream().forEach(
				value -> System.out.format("      '%1$s'%n", value));
		});
		return response;
	}

	private HttpResponse<String> setCookies(HttpResponse<String> response) {
		cookies = response.headers().allValues("set-cookie").stream()
			.distinct()
			.map(HttpCookie::parse)
			.flatMap(List::stream)
			.collect(Collectors.toList());
		System.out.format("Recieved cookies:%n");
		cookies.stream().forEach(
			cookie -> System.out.format("   '%1$s'%n", cookie));
		return response;
	}

	private String cookieHeaderValue() {
		String result = (cookies == null || cookies.isEmpty())
			? ""
			: cookies.stream()
				.map(Object::toString)
				.collect(Collectors.joining("; "));
		System.out.format("Cookie: %1$s%n", result);
		return result;
	}

	public static void main(String [] args) {
		ScilympiadRetriever retriever = new ScilympiadRetriever();
		retriever.getReport();
		//System.out.format("Found %1$d students:%n", students.size());
		//students.forEach(student -> System.out.format("   %1$s%n", student));
	}
}
