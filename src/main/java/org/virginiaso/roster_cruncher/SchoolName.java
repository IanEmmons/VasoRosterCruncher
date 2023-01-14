package org.virginiaso.roster_cruncher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;

public class SchoolName {
	private static class SchoolNameHolder {
		private static final SchoolName INSTANCE = new SchoolName();
	}

	private static final String RESOURCE_NAME = "school-names.csv";
	private static final String SCILYMPIAD_NAME_COLUMN = "ScilympiadName";
	private static final String CANONICAL_NAME_COLUMN = "CanonicalName";

	// Maps Scilympiad school name to Portal school name (a.k.a., canonical name)
	private final Map<String, String> translations;

	/**
	 * Get the singleton instance of Config. This follows the "lazy initialization
	 * holder class" idiom for lazy initialization of a static field. See Item 83 of
	 * Effective Java, Third Edition, by Joshua Bloch for details.
	 *
	 * @return the instance
	 */
	private static SchoolName inst() {
		return SchoolNameHolder.INSTANCE;
	}

	private SchoolName() {
		try (
			var is = Util.getResourceAsInputStream(RESOURCE_NAME);
			var parser = CSVParser.parse(is, Util.CHARSET, Util.CSV_FORMAT);
		) {
			translations = parser.stream()
				.filter(record -> StringUtils.isNotBlank(record.get(SCILYMPIAD_NAME_COLUMN)))
				.collect(Collectors.toMap(
					record -> record.get(SCILYMPIAD_NAME_COLUMN),	// key mapper
					record -> record.get(CANONICAL_NAME_COLUMN),		// value mapper
					(v1, v2) -> {
						throw new SchoolNameException(
							"One Scilympid school name is mapped to two canonical school names, '%1$s' and '%2$s'",
							v1, v2);
					}));	// merge function

			var canonicalSchoolNames = getCanonicalSchoolNames();
			ensureCanonicalNamesAreInPortal(translations, canonicalSchoolNames);
			addIdentityMappings(translations, canonicalSchoolNames);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static Set<String> getCanonicalSchoolNames() throws IOException {
		var reportDir = Config.inst().getPortalReportDir();
		if (!reportDir.isDirectory()) {
			return Collections.emptySet();
		}

		try (var stream = Files.find(reportDir.toPath(), Integer.MAX_VALUE,
			(path, attrs) -> attrs.isRegularFile(), FileVisitOption.FOLLOW_LINKS)) {
			if (stream.findAny().isEmpty()) {
				return Collections.emptySet();
			}
		}

		return ConsolidatedCoachRetriever.getConsolidatedCoachList().stream()
			.map(Coach::school)
			.collect(Collectors.toUnmodifiableSet());
	}

	private static void ensureCanonicalNamesAreInPortal(Map<String, String> ensureCanonicalNames,
		Set<String> canonicalSchoolNames) {

		var errMsg = ensureCanonicalNames.values().stream()
			.filter(school -> !canonicalSchoolNames.contains(school))
			.sorted()
			.collect(Collectors.joining("%n   ".formatted()));
		if (!errMsg.isEmpty()) {
			throw new SchoolNameException(
				"These canonical school names are not present in the portal:%n   %1$s",
				errMsg);
		}
	}

	private static void addIdentityMappings(Map<String, String> translations,
		Set<String> canonicalSchoolNames) {

		canonicalSchoolNames.stream()
			.forEach(school -> translations.put(school, school));
	}

	public static void checkForUnmappedScylimpiadSchools(Set<String> scylimpiadSchools) {
		var errMsg = scylimpiadSchools.stream()
			.filter(school -> !inst().translations.containsKey(school))
			.collect(Collectors.joining("%n   ".formatted()));
		if (!errMsg.isEmpty()) {
			throw new SchoolNameException(
				"These Scilympid schools have not been mapped to a canonical name:%n   %1$s",
				errMsg);
		}
	}

	public static String normalize(String scilympiadSchoolName) {
		var canonicalName = inst().translations.get(scilympiadSchoolName);
		if (canonicalName == null) {
			throw new SchoolNameException(
				"The Scilympid school name '%1$s' has not been mapped to a canonical name",
				scilympiadSchoolName);
		}
		return canonicalName;
	}
}
