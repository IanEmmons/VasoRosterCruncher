package org.virginiaso.roster_diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.tuple.Pair;

public class School {
	private static final String RESOURCE_NAME = "SchoolNameNormalizations.csv";
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
		.setHeader()
		.setIgnoreEmptyLines(true)
		.setTrim(true)
		.build();
	private static final List<Pair<String, String>> TRANSLATIONS;

	static {
		try (
			InputStream is = Util.getResourceAsInputStream(RESOURCE_NAME);
			CSVParser parser = CSVParser.parse(is, CHARSET, FORMAT);
		) {
			TRANSLATIONS = parser.stream()
				.map(record -> Pair.of(record.get("From").toLowerCase(), record.get("To").toLowerCase()))
				.collect(Collectors.toUnmodifiableList());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private School() {}	// prevent instantiation

	public static String normalize(String schoolName) {
		schoolName = schoolName.toLowerCase();
		schoolName = Util.normalizeSpace(schoolName);
		for (Pair<String, String> translation : TRANSLATIONS) {
			schoolName = schoolName.replace(translation.getLeft(), translation.getRight());
		}
		return schoolName;
	}
}
