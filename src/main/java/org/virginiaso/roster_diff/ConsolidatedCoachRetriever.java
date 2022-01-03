package org.virginiaso.roster_diff;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVParser;

public class ConsolidatedCoachRetriever {
	private static final String RESOURCE_NAME = "coaches-extra.csv";
	private static final String SCHOOL_COLUMN = "School Name";
	private static final String NAME_COLUMN = "Coach Name";
	private static final String EMAIL_COLUMN = "Head Coach Email";

	private ConsolidatedCoachRetriever() {}	// prevent instantiation

	public static List<Coach> getConsolidatedCoachList() throws IOException {
		var portalCoaches = CoachRetrieverFactory.create().readLatestReportFile();
		var extraCoaches = getExtraCoachList();

		var portalSchools = portalCoaches.stream()
			.map(Coach::school)
			.collect(Collectors.toUnmodifiableSet());
		var schoolsNotInPortal = extraCoaches.stream()
			.map(Coach::school)
			.filter(school -> !portalSchools.contains(school))
			.collect(Collectors.toUnmodifiableSet());
		if (!schoolsNotInPortal.isEmpty()) {
			var schoolList = schoolsNotInPortal.stream().collect(
				Collectors.joining("%n   ".formatted()));
			System.out.format(
				"Found %1$d schools in 'extra' coach list not in the portal:%n   %2$s%n",
				schoolsNotInPortal.size(), schoolList);
			throw new IllegalStateException("Need to fix the 'extra' coach list");
		}

		var portalEmails = portalCoaches.stream()
			.map(Coach::email)
			.map(String::toLowerCase)
			.collect(Collectors.toUnmodifiableSet());
		var extraCoachesNoDups = extraCoaches.stream()
			.filter(coach -> !portalEmails.contains(coach.email().toLowerCase()))
			.collect(Collectors.toUnmodifiableList());

		return Stream.concat(portalCoaches.stream(), extraCoachesNoDups.stream())
			.collect(Collectors.toUnmodifiableList());
	}

	private static List<Coach> getExtraCoachList() throws IOException {
		try (
			var is = Util.getResourceAsInputStream(RESOURCE_NAME);
			var parser = CSVParser.parse(is, Util.CHARSET, Util.CSV_FORMAT);
		) {
			return parser.stream()
				.map(record -> new Coach(
					record.get(NAME_COLUMN),
					record.get(EMAIL_COLUMN),
					record.get(SCHOOL_COLUMN)))
				.collect(Collectors.toUnmodifiableList());
		}
	}
}
