package org.virginiaso.roster_cruncher;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVParser;

public class ConsolidatedCoachRetriever {
	private static final String RESOURCE_NAME = "coaches-extra.csv";
	private static final String SCHOOL_COLUMN = "School Name";
	private static final String NAME_COLUMN = "Coach Name";
	private static final String EMAIL_COLUMN = "Head Coach Email";

	private ConsolidatedCoachRetriever() {}	// prevent instantiation

	public static Set<Coach> getConsolidatedCoachList() throws IOException {
		var portalCoaches = CoachRetrieverFactory.create().readLatestReportFile();
		var extraCoaches = getExtraCoachList();

		var portalSchools = portalCoaches.stream()
			.map(Coach::school)
			.collect(Collectors.toUnmodifiableSet());

		var extraCoachesForSchoolsFoundInPortal = extraCoaches.stream()
			.filter(coach -> portalSchools.contains(coach.school()));

		return Stream.concat(portalCoaches.stream(), extraCoachesForSchoolsFoundInPortal)
			.filter(coach -> !coach.email().equalsIgnoreCase("quinn.mcfee@gmail.com"))
			.collect(Collectors.toCollection(() -> new TreeSet<>()));
	}

	private static List<Coach> getExtraCoachList() throws IOException {
		Stopwatch timer = new Stopwatch();
		try (
			var is = Util.getResourceAsInputStream(RESOURCE_NAME);
			var parser = CSVParser.parse(is, Util.CHARSET, Util.CSV_FORMAT);
		) {
			var result = parser.stream()
				.map(record -> new Coach(
					record.get(NAME_COLUMN),
					record.get(EMAIL_COLUMN),
					record.get(SCHOOL_COLUMN)))
				.collect(Collectors.toUnmodifiableList());
			timer.stopAndReport("Parsed extra coach file");
			return result;
		}
	}
}
