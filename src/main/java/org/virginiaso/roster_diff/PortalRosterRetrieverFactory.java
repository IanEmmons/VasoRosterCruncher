package org.virginiaso.roster_diff;

import java.lang.reflect.Type;

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

public class PortalRosterRetrieverFactory {
	private static class PortalStudentDeserializer implements JsonDeserializer<PortalStudent> {
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
	}

	private static class PortalStudentSerializer implements JsonSerializer<PortalStudent> {
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

	private PortalRosterRetrieverFactory() {}	// prevent instantiation

	public static PortalRetriever<PortalStudent> create() {
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(PortalStudent.class, new PortalStudentDeserializer())
			.registerTypeAdapter(PortalStudent.class, new PortalStudentSerializer())
			.create();
		return new PortalRetriever<PortalStudent>(gson, "portal", 503, 1151);
	}
}
