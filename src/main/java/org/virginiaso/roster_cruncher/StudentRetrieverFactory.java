package org.virginiaso.roster_cruncher;

import java.lang.reflect.Type;

import org.virginiaso.roster_cruncher.PortalRetriever.ReportResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class StudentRetrieverFactory {
	private static class StudentSerializer implements JsonSerializer<Student>,
			JsonDeserializer<Student> {
		@Override
		public JsonElement serialize(Student src, Type typeOfSrc,
				JsonSerializationContext context) {
			JsonObject name = new JsonObject();
			name.add("first", new JsonPrimitive(src.firstName()));
			name.add("last", new JsonPrimitive(src.lastName()));

			JsonObject school = new JsonObject();
			school.add("id", new JsonPrimitive("unknown"));
			school.add("identifier", new JsonPrimitive(src.school()));

			JsonArray schoolArray = new JsonArray();
			schoolArray.add(school);

			JsonObject result = new JsonObject();
			result.add("id", new JsonPrimitive("unknown"));
			result.add("field_52", name);
			result.add("field_70", new JsonPrimitive(src.nickName()));
			result.add("field_56", schoolArray);
			result.add("field_90", new JsonPrimitive(src.grade()));
			return result;
		}

		@Override
		public Student deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) {
			String firstName = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_52").getAsJsonObject()
				.get("first").getAsString());
			String lastName = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_52").getAsJsonObject()
				.get("last").getAsString());
			String nickName = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_70").getAsString());
			String school = Util.normalizeSpace(json.getAsJsonObject()
				.get("field_56").getAsJsonArray()
				.get(0).getAsJsonObject()
				.get("identifier").getAsString());
			int grade = json.getAsJsonObject()
				.get("field_90").getAsInt();
			return new Student(firstName, lastName, nickName, school, grade);
		}
	}

	private StudentRetrieverFactory() {}	// prevent instantiation

	public static PortalRetriever<Student> create() {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(Student.class, new StudentSerializer())
			.create();
		return new PortalRetriever<>(gson, "roster",
			new TypeToken<ReportResponse<Student>>(){}.getType());
	}
}
