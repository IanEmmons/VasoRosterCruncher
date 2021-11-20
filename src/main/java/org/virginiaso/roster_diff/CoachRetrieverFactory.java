package org.virginiaso.roster_diff;

import java.lang.reflect.Type;

import org.virginiaso.roster_diff.PortalRetriever.ReportResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class CoachRetrieverFactory {
	private static class CoachSerializer implements JsonSerializer<Coach>,
			JsonDeserializer<Coach> {
		@Override
		public JsonElement serialize(Coach src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject name = new JsonObject();
			name.add("first", new JsonPrimitive(src.firstName()));
			name.add("last", new JsonPrimitive(src.lastName()));

			JsonObject email = new JsonObject();
			email.add("email", new JsonPrimitive(src.email()));

			JsonObject phone = new JsonObject();
			phone.add("formatted", new JsonPrimitive((src.phone() == null) ? "" : src.phone()));

			JsonObject school = new JsonObject();
			school.add("id", new JsonPrimitive("unknown"));
			school.add("identifier", new JsonPrimitive(src.school()));

			JsonArray schoolArray = new JsonArray();
			schoolArray.add(school);

			JsonObject result = new JsonObject();
			result.add("id", new JsonPrimitive("unknown"));
			result.add("field_96", name);
			result.add("field_97", email);
			result.add("field_1148", phone);
			result.add("field_106", schoolArray);
			return result;
		}

		@Override
		public Coach deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			String firstName = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_96").getAsJsonObject()
				.get("first").getAsString());
			String lastName = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_96").getAsJsonObject()
				.get("last").getAsString());
			String email = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_97").getAsJsonObject()
				.get("email").getAsString());
			String phone = getPhoneFromField(json.getAsJsonObject().get("field_1148"));
			String school = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_106").getAsJsonArray()
				.get(0).getAsJsonObject()
				.get("identifier").getAsString());
			return new Coach(firstName, lastName, email, phone, school);
		}

		private String getPhoneFromField(JsonElement phoneField) {
			if (phoneField.isJsonObject()) {
				return Util.normalizeSpace(phoneField.getAsJsonObject()
					.get("formatted").getAsString());
			} else {
				return null;
			}
		}
	}

	private CoachRetrieverFactory() {}	// prevent instantiation

	public static PortalRetriever<Coach> create() {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(Coach.class, new CoachSerializer())
			.create();
		return new PortalRetriever<Coach>(
			new TypeToken<ReportResponse<Coach>>(){}.getType(),
			gson, "coaches", 483, 1078);
	}
}
