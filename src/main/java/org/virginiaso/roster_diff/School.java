package org.virginiaso.roster_diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.tuple.Pair;

public class School {
	static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
		.setHeader()
		.setIgnoreEmptyLines(true)
		.setTrim(true)
		.build();
	private static final String TRANSLATION_RESOURCE = "SchoolNameNormalizations.csv";
	private static final List<Pair<String, String>> TRANSLATIONS;

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
	}

	public static String normalize(String schoolName) {
		schoolName = schoolName.toLowerCase();
		schoolName = Util.normalizeSpace(schoolName);
		for (Pair<String, String> translation : TRANSLATIONS) {
			schoolName = schoolName.replace(translation.getLeft(), translation.getRight());
		}
		return schoolName;
	}
}
