package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class App {
	private final File masterReportFile;
	private final boolean sendReports;

	public static void main(String[] args) {
		try {
			App app = new App(args);
			app.run();
		} catch (CmdLineException ex) {
			if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
				System.out.format("%n%1$s%n%n", ex.getMessage());
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private App(String[] args) throws CmdLineException {
		Properties props = Util.loadPropertiesFromResource(Util.CONFIGURATION_RESOURCE);
		masterReportFile = Util.parseFileArgument(props, "master.report.file");
		sendReports = Boolean.parseBoolean(props.getProperty("send.reports", "false"));
	}

	private void run() throws IOException, ParseException {
		List<Coach> coaches = CoachRetrieverFactory.create().readLatestReportFile();
		Map<String, List<Coach>> schoolToCoachsMap = coaches.stream()
			.collect(Collectors.groupingBy(coach -> School.normalize(coach.school())));
		List<Match> matches = Match.parse(masterReportFile);
		List<Student> pStudents = StudentRetrieverFactory.create().readLatestReportFile();
		List<Student> sStudents = ScilympiadParser.readLatestRosterFile();

		checkForMissingSchoolsInCoachesFile(schoolToCoachsMap.keySet(), pStudents, sStudents);

		System.out.format("Found %1$d portal students and %2$d Scilimpiad students%n",
			pStudents.size(), sStudents.size());

		DifferenceEngine engine = DifferenceEngine.compare(matches, pStudents, sStudents,
			new WeightAvgDistanceFunction());

		//System.out.print(engine.formatDistanceHistogram());
		EnumMap<Verdict, Long> verdictCounts = engine.getMatches().stream()
			.collect(Collectors.groupingBy(
				Match::getVerdict,						// classifier
				() -> new EnumMap<>(Verdict.class),	// map factory
				Collectors.counting()));				// downstream collector
		System.out.format("Marked Same:      %1$3d%n",
			verdictCounts.getOrDefault(Verdict.SAME, 0L));
		System.out.format("Marked Different: %1$3d%n",
			verdictCounts.getOrDefault(Verdict.DIFFERENT, 0L));
		System.out.format("Exact matches:    %1$3d%n",
			verdictCounts.getOrDefault(Verdict.EXACT_MATCH, 0L));
		System.out.format("Portal students not in Scilympiad:     %1$3d%n",
			engine.getPStudentsNotFoundInS().size());
		System.out.format("Scilympiad students not in the Portal: %1$3d%n",
			engine.getSStudentsNotFoundInP().size());

		Stopwatch reportTimer = new Stopwatch();
		ReportBuilder rb = new ReportBuilder(engine, masterReportFile, getReportDir());
		rb.createReport(null, null, false);

		schoolToCoachsMap.entrySet().stream().forEach(
			schoolEntry -> rb.createReport(schoolEntry.getKey(), schoolEntry.getValue(), sendReports));
		reportTimer.stopAndReport("Built reports");
	}

	private void checkForMissingSchoolsInCoachesFile(Set<String> schools,
			List<Student> pStudents, List<Student> sStudents) {
		Set<String> schoolNames = new TreeSet<>();
		pStudents.stream()
			.map(pStudent -> pStudent.school())
			.forEach(schoolNames::add);
		sStudents.stream()
			.map(sStudent -> sStudent.school())
			.forEach(schoolNames::add);
		List<String> unknownSchools = schoolNames.stream()
			.filter(schoolName -> schools.stream().noneMatch(
				school -> school.equalsIgnoreCase(schoolName)))
			.collect(Collectors.toUnmodifiableList());
		if (!unknownSchools.isEmpty()) {
			System.out.format("Schools not in coach file (%1$d):%n", unknownSchools.size());
			unknownSchools.forEach(name -> System.out.format("   %1$s%n", name));
		}
	}

	private static File getReportDir() {
		File reportDir = new File("reports-%1$TF_%1$TT"
			.formatted(System.currentTimeMillis())
			.replace(':', '-'));
		if (reportDir.exists()) {
			throw new IllegalStateException("Report directory '%1$s' already exists"
				.formatted(reportDir.getPath()));
		}
		return reportDir;
	}
}
