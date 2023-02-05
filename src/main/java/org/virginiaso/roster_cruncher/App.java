package org.virginiaso.roster_cruncher;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
	private final File masterReportFile;

	public static void main(String[] args) {
		try {
			App app = new App(args);
			app.run();
		} catch (CmdLineException ex) {
			if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
				System.out.format("%n%1$s%n%n", ex.getMessage());
			}
		} catch (SchoolNameException ex) {
			System.out.println(ex.getMessage());
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	/** @throws CmdLineException If the syntax of the command line is incorrect. */
	private App(String[] args) throws CmdLineException {
		masterReportFile = Config.inst().getMasterReportFile();
	}

	private void run() throws IOException, ParseException {
		List<Match> matches = Match.parse(masterReportFile);
		Map<String, String> schoolNameMapping = SchoolNameMappingParser.parse(masterReportFile);
		Set<Coach> extraCoaches = ExtraCoachParser.parse(masterReportFile);
		Set<String> ignoredCoaches = IgnoredCoachParser.parse(masterReportFile);
		Set<Student> ignoredSStudents = IgnoredScilympiadStudentParser.parse(masterReportFile);
		List<Student> pStudents = StudentRetrieverFactory.create().readLatestReportFile();
		Set<Student> sStudents = ScilympiadParser.readLatestRosterFile(schoolNameMapping);

		Set<Coach> coaches = getConsolidatedCoachList(extraCoaches);
		Map<String, List<Coach>> schoolToCoachsMap = coaches.stream().collect(
			Collectors.groupingBy(Coach::school, TreeMap::new, Collectors.toList()));

		sStudents = sStudents.stream()
			.filter(student -> !ignoredSStudents.contains(student))
			.collect(Collectors.toUnmodifiableSet());

		checkForMissingSchoolsInCoachesFile(schoolToCoachsMap.keySet(), pStudents, sStudents);

		System.out.format("Found %1$d portal students and %2$d Scilympiad students%n",
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
		ReportBuilder rb = new ReportBuilder(engine, sStudents, extraCoaches, ignoredCoaches,
			masterReportFile, getReportDir());
		rb.createMasterReport(schoolNameMapping, ignoredSStudents);

		schoolToCoachsMap.entrySet().stream().forEach(
			schoolEntry -> rb.createSchoolReport(
				schoolEntry.getKey(), schoolEntry.getValue(), Config.inst().sendReports()));
		reportTimer.stopAndReport("Built reports");
	}

	public static Set<Coach> getConsolidatedCoachList(Set<Coach> extraCoaches) throws IOException {
		var portalCoaches = CoachRetrieverFactory.create().readLatestReportFile();

		var portalSchools = portalCoaches.stream()
			.map(Coach::school)
			.collect(Collectors.toUnmodifiableSet());

		var extraCoachesForSchoolsFoundInPortal = extraCoaches.stream()
			.filter(coach -> portalSchools.contains(coach.school()));

		return Stream.concat(portalCoaches.stream(), extraCoachesForSchoolsFoundInPortal)
			.collect(Collectors.toCollection(TreeSet::new));
	}

	private static void checkForMissingSchoolsInCoachesFile(Set<String> schools,
			List<Student> pStudents, Set<Student> sStudents) {
		var schoolNames = Stream.concat(pStudents.stream(), sStudents.stream())
			.map(Student::school)
			.collect(Collectors.toCollection(TreeSet::new));
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
