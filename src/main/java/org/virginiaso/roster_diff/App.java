package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

public class App {
	private final File coachesFile;
	private final File masterReportFile;
	private final File portalFile;
	private final File scilympiadFile;

	public static void main(String[] args) {
		try {
			App app = new App(args);
			app.run();
		} catch (CmdLineException ex) {
			usage(ex.getMessage());
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private static void usage(String message) {
		System.out.format("%n");
		if (message != null && !message.isBlank()) {
			System.out.format("%1$s%n%n", message);
		}
		System.out.format(
			"Usage: %1$s <coaches-file> <report-all-file> <portal-file> <scilympiad-file>%n%n",
			App.class.getName());
	}

	private App(String[] args) throws CmdLineException {
		if (args.length < 4) {
			throw new CmdLineException("Too few arguments");
		} else if (args.length > 4) {
			throw new CmdLineException("Too many arguments");
		}
		coachesFile = parseRequiredFileArgument(args[0]);
		masterReportFile = parseFileArgument(args[1]);
		portalFile = parseRequiredFileArgument(args[2]);
		scilympiadFile = parseRequiredFileArgument(args[3]);
	}

	private static File parseFileArgument(String arg) {
		return new File(arg.trim());
	}

	private static File parseRequiredFileArgument(String arg) throws CmdLineException {
		File result = parseFileArgument(arg);
		if (!result.exists()) {
			throw new CmdLineException("'%1$s' does not exist", arg);
		} else if (!result.isFile()) {
			throw new CmdLineException("'%1$s' is not a regular file", arg);
		}
		return result;
	}

	private void run() throws IOException, ParseException {
		List<School> schools = School.parse(coachesFile);
		List<Match> matches = Match.parse(masterReportFile);
		List<PortalStudent> pStudents = PortalStudent.parse(portalFile);
		List<ScilympiadStudent> sStudents = ScilympiadStudent.parse(scilympiadFile);

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

		Stopwatch masterReportTimer = new Stopwatch();
		ReportBuilder rb = new ReportBuilder(engine, masterReportFile, getReportDir());
		rb.createReport(null, false);
		masterReportTimer.stopAndReport("Built master report");

		// TODO: Get this from configuration:
		boolean sendEmail = false;

		Stopwatch schoolReportTimer = new Stopwatch();
		schools.stream().forEach(school -> rb.createReport(school, sendEmail));
		schoolReportTimer.stopAndReport("Built school reports");
	}

	private static File getReportDir() {
		File reportDir = new File(String
			.format("reports-%1$TF_%1$TT", System.currentTimeMillis())
			.replace(':', '-'));
		if (reportDir.exists()) {
			throw new IllegalStateException(String.format(
				"Report directory '%1$s' already exists", reportDir.getPath()));
		}
		return reportDir;
	}
}
