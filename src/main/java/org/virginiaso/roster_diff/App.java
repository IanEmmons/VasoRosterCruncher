package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class App {
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
			"Usage: %1$s <portal-students-file> <scilympiad_students-file>%n%n",
			App.class.getName());
	}

	private App(String[] args) throws CmdLineException {
		if (args.length < 2) {
			throw new CmdLineException("Too few arguments");
		} else if (args.length > 2) {
			throw new CmdLineException("Too many arguments");
		}
		portalFile = parseFileArgument(args[0]);
		scilympiadFile = parseFileArgument(args[1]);
	}

	private static File parseFileArgument(String arg) throws CmdLineException {
		File result = new File(arg.trim());
		if (!result.exists()) {
			throw new CmdLineException("'%1$s' does not exist", arg);
		} else if (!result.isFile()) {
			throw new CmdLineException("'%1$s' is not a regular file", arg);
		}
		return result;
	}

	private void run() throws IOException, InvalidFormatException, ParseException {
		List<PortalStudent> pStudents = PortalStudent.parse(portalFile);
		List<ScilympiadStudent> sStudents = ScilympiadStudent.parse(scilympiadFile);

		System.out.format("Found %1$d portal students and %2$d Scilimpiad students%n",
			pStudents.size(), sStudents.size());

		DifferenceEngine engine = DifferenceEngine.compare(pStudents, sStudents,
			new WeightAvgDistanceFunction());

		System.out.print(engine.formatDistanceHistogram());
		System.out.format("Exact matches: %1$d%n", engine.getExactMatches().size());
		System.out.format("Portal students not in Scilympiad: %1$d%n", engine.getPStudentsNotFoundInS().size());
		//engine.getpStudentsNotFoundInS().stream().forEach(pStudent -> System.out.format("   %1$s%n", pStudent));
		System.out.format("Scilympiad students not in the Portal: %1$d%n", engine.getSStudentsNotFoundInP().size());
		//engine.getpStudentsNotFoundInS().stream().forEach(sStudent -> System.out.format("   %1$s%n", sStudent));

		for (Map.Entry<ScilympiadStudent, Map<Integer, List<PortalStudent>>> entry : engine.getResults().entrySet()) {
			reportMatch(entry.getKey(), entry.getValue());
		}

		ReportBuilder.newReport(engine, "all");
	}

	private static void reportMatch(ScilympiadStudent sStudent,
			Map<Integer, List<PortalStudent>> matches) {
		System.out.format("%n%1$s:%n%2$s", sStudent, matches.entrySet().stream()
			.map(entry -> reportMatchHelper(entry.getKey(), entry.getValue()))
			.collect(Collectors.joining()));
	}

	private static String reportMatchHelper(int distance, List<PortalStudent> pStudents) {
		return pStudents.stream()
			.map(pStudent -> String.format("   %1$d: %2$s%n", distance, pStudent))
			.collect(Collectors.joining());
	}
}
