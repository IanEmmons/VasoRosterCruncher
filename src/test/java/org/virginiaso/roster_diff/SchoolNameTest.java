package org.virginiaso.roster_diff;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;

public class SchoolNameTest {
	@Test
	public void allPortalSchoolsPresentTest() throws IOException {
		var props = Util.loadPropertiesFromResource(Util.CONFIGURATION_RESOURCE);
		var reportDir = Util.parseFileArgument(props, "portal.report.dir");
		if (!reportDir.isDirectory()) {
			return;
		}

		try (var stream = Files.find(reportDir.toPath(), Integer.MAX_VALUE,
			(path, attrs) -> attrs.isRegularFile(), FileVisitOption.FOLLOW_LINKS)) {
			if (stream.findAny().isEmpty()) {
				return;
			}
		}

		var portalSchools = ConsolidatedCoachRetriever.getConsolidatedCoachList().stream()
			.map(Coach::school)
			.collect(Collectors.toUnmodifiableSet());
		var canonicalSchools = getCanonicalSchools();

		var portalSchoolsNotInCanonical = Util.setDiff(portalSchools, canonicalSchools);
		assertTrue(portalSchoolsNotInCanonical.isEmpty(),
			"Some schools in the portal are not in the canonical list (%1$s): %n\t'%2$s'"
				.formatted(SchoolName.RESOURCE_NAME, portalSchoolsNotInCanonical.stream()
					.collect(Collectors.joining("',%n\t'".formatted()))));
	}

	private static Set<String> getCanonicalSchools() throws IOException {
		try (
			InputStream is = Util.getResourceAsInputStream(SchoolName.RESOURCE_NAME);
			CSVParser parser = CSVParser.parse(is, Util.CHARSET, Util.CSV_FORMAT);
		) {
			return parser.stream()
				.map(record -> record.get(SchoolName.CANONICAL_NAME_COLUMN))
				.collect(Collectors.toUnmodifiableSet());
		}
	}
}
