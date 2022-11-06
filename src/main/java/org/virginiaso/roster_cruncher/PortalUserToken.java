package org.virginiaso.roster_cruncher;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PortalUserToken {
	private static class PortalUserTokenHolder {
		private static final PortalUserToken INSTANCE = new PortalUserToken();
	}

	private static final String TOKEN_URL = "https://api.knack.com/v1/applications/%1$s/session";
	private static final String TOKEN_BODY = "{\"email\":\"%1$s\",\"password\":\"%2$s\"}";

	private final String user;
	private final String password;
	private final String applicationId;
	private String userToken;

	/**
	 * Get the singleton instance of PortalUserToken. This follows the "lazy
	 * initialization holder class" idiom for lazy initialization of a static field.
	 * See Item 83 of Effective Java, Third Edition, by Joshua Bloch for details.
	 *
	 * @return the instance
	 */
	public static PortalUserToken inst() {
		return PortalUserTokenHolder.INSTANCE;
	}

	private PortalUserToken() {
		var props = Util.loadPropertiesFromResource(Util.CONFIGURATION_RESOURCE);
		var appName = props.getProperty("portal.application.name");
		user = props.getProperty("portal.user");
		password = props.getProperty("portal.password");
		applicationId = props.getProperty("portal.%1$s.application.id".formatted(appName));
		userToken = null;
		fetchUserToken();
		System.out.format("Found user token '%1$s'%n", userToken);
	}

	private void fetchUserToken() {
		var url = TOKEN_URL.formatted(applicationId);
		var requestBody = TOKEN_BODY.formatted(user, password);
		var httpRequest = HttpRequest.newBuilder(URI.create(url))
			.POST(BodyPublishers.ofString(requestBody))
			.header("Content-Type", Util.JSON_MEDIA_TYPE)
			.build();
		HttpClient.newHttpClient()
			.sendAsync(httpRequest, BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(this::interpretResponse)
			.join();
	}

	private void interpretResponse(String jsonResponseBody) {
		JsonObject response = JsonParser.parseString(jsonResponseBody).getAsJsonObject();
		if (response.get("session") != null) {
			userToken = response
				.get("session").getAsJsonObject()
				.get("user").getAsJsonObject()
				.get("token").getAsString();
		} else {
			JsonArray errors = response.get("errors").getAsJsonArray();
			var errorMessages = Util.asStream(errors)
				.map(JsonElement::getAsJsonObject)
				.map(error -> error.get("message"))
				.map(JsonElement::getAsString)
				.collect(Collectors.joining(
					"%n".formatted(),
					"Unable to retrieve Portal API token: ",
					""));
			throw new IllegalStateException(errorMessages);
		}
	}

	public String getApplicationId() {
		return applicationId;
	}

	public String getUserToken() {
		return userToken;
	}
}
