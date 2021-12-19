package org.virginiaso.roster_diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

public class SchoolName {
	static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
		.setHeader()
		.setIgnoreEmptyLines(true)
		.setTrim(true)
		.build();
	static final String TRANSLATION_RESOURCE = "school-names.csv";
	private static final Map<String, String> TRANSLATIONS;

	private SchoolName() {}	// prevent instantiation

	static {
		try (
			InputStream is = Util.getResourceAsInputStream(TRANSLATION_RESOURCE);
			CSVParser parser = CSVParser.parse(is, Util.CHARSET, FORMAT);
		) {
			TRANSLATIONS = parser.stream()
				.filter(record -> {
					var sSchool = record.get("ScilympiadName");
					return sSchool != null && !sSchool.isBlank();
				})
				.collect(Collectors.toUnmodifiableMap(
					record -> record.get("ScilympiadName"),	// key mapper
					record -> record.get("CanonicalName"),		// value mapper
					(v1, v2) -> {
						throw new IllegalStateException("One Scilympid school name is mapped to "
							+ "two canonical school names, '%1$s' and '%2$s'.".formatted(v1, v2));
					}));	// merge function
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static String normalize(String scilympiadSchoolName) {
		var canonicalName = TRANSLATIONS.get(scilympiadSchoolName);
		if (canonicalName == null) {
			throw new IllegalStateException(
				"The Scilympid school name '%1$s' has not been mapped to a canonical name."
					.formatted(scilympiadSchoolName));
		}
		return canonicalName;
	}
}
