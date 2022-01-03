package org.virginiaso.roster_diff;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;

public class SchoolName {
	static final String RESOURCE_NAME = "school-names.csv";
	private static final String SCILYMPIAD_NAME_COLUMN = "ScilympiadName";
	static final String CANONICAL_NAME_COLUMN = "CanonicalName";
	private static final Map<String, String> TRANSLATIONS;

	private SchoolName() {}	// prevent instantiation

	static {
		try (
			var is = Util.getResourceAsInputStream(RESOURCE_NAME);
			var parser = CSVParser.parse(is, Util.CHARSET, Util.CSV_FORMAT);
		) {
			TRANSLATIONS = parser.stream()
				.filter(record -> {
					var sSchool = record.get(SCILYMPIAD_NAME_COLUMN);
					return sSchool != null && !sSchool.isBlank();
				})
				.collect(Collectors.toUnmodifiableMap(
					record -> record.get(SCILYMPIAD_NAME_COLUMN),	// key mapper
					record -> record.get(CANONICAL_NAME_COLUMN),		// value mapper
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
