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

		PortalRetriever<Coach> coachRetriever = CoachRetrieverFactory.create();
		Set<String> portalSchools = coachRetriever.readLatestReportFile().stream()
			.map(Coach::school)
			.collect(Collectors.toUnmodifiableSet());

		Set<String> canonicalSchools;
		try (
			InputStream is = Util.getResourceAsInputStream(SchoolName.TRANSLATION_RESOURCE);
			CSVParser parser = CSVParser.parse(is, Util.CHARSET, SchoolName.FORMAT);
		) {
			canonicalSchools = parser.stream()
				.map(record -> record.get("CanonicalName"))
				.collect(Collectors.toUnmodifiableSet());
		}

		assertTrue(canonicalSchools.containsAll(portalSchools),
			"Some schools in the portal are not in the canonical list (%1$s)"
				.formatted(SchoolName.TRANSLATION_RESOURCE));
		assertTrue(portalSchools.containsAll(canonicalSchools),
			"Some schools in the canonical list (%1$s) are not in the portal"
				.formatted(SchoolName.TRANSLATION_RESOURCE));
	}
}
