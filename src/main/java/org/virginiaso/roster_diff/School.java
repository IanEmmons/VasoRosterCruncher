package org.virginiaso.roster_diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;

public class School {
	static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
		.setHeader()
		.setIgnoreEmptyLines(true)
		.setTrim(true)
		.build();
	private static final String TRANSLATION_RESOURCE = "SchoolNameNormalizations.csv";
	private static final List<Pair<String, String>> TRANSLATIONS;
	private static final String SCHOOLS_RESOURCE = "coaches.csv";
	private static final String[] EMAIL_COLUMNS = { "Coach 1", "Coach 2", "Coach 3" };
	private static final List<School> SCHOOLS;

	public final String name;
	public final String normalizedName;
	public final List<String> coachEmails;

	static {
		try (
			InputStream is = Util.getResourceAsInputStream(TRANSLATION_RESOURCE);
			CSVParser parser = CSVParser.parse(is, Util.CHARSET, FORMAT);
		) {
			TRANSLATIONS = parser.stream()
				.map(record -> Pair.of(record.get("From").toLowerCase(), record.get("To").toLowerCase()))
				.collect(Collectors.toUnmodifiableList());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}

		try (
			InputStream is = Util.getResourceAsInputStream(SCHOOLS_RESOURCE);
			CSVParser parser = CSVParser.parse(is, Util.CHARSET, FORMAT);
		) {
			SCHOOLS = parser.stream()
				.map(School::new)
				.collect(Collectors.toUnmodifiableList());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static String normalize(String schoolName) {
		schoolName = schoolName.toLowerCase();
		schoolName = Util.normalizeSpace(schoolName);
		for (Pair<String, String> translation : TRANSLATIONS) {
			schoolName = schoolName.replace(translation.getLeft(), translation.getRight());
		}
		return schoolName;
	}

	public static List<School> getSchools() {
		return SCHOOLS;
	}

	private School(CSVRecord record) {
		name = Util.normalizeSpace(record.get("School"));
		normalizedName = School.normalize(name);
		coachEmails = Arrays.stream(EMAIL_COLUMNS)
			.filter(record::isSet)
			.map(record::get)
			.map(Util::normalizeSpace)
			.filter(email -> email != null && !email.isEmpty())
			.collect(Collectors.toList());
	}
}
