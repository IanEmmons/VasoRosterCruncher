package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class App {
	private final File portalFile;
	private final File scilympiadFile;

	public static String getGreeting() {
		return "Hello World!";
	}

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

		Set<String> schools = new TreeSet<>();
		pStudents.stream()
			.map(student -> SchoolNameNormalizer.normalize(student.school))
			.forEach(schools::add);
		sStudents.stream()
			.map(student -> SchoolNameNormalizer.normalize(student.school))
			.forEach(schools::add);
		System.out.format("Schools (%1$d):%n", schools.size());
		schools.stream().forEach(school -> System.out.format("   '%1$s'%n", school));

		DifferenceEngine engine = DifferenceEngine.compare(pStudents, sStudents,
			new WeightAvgDistanceFunction());

		Map<Integer, Integer> distanceCounts = new TreeMap<>();
		for (Map.Entry<ScilympiadStudent, List<Pair<PortalStudent, Integer>>> entry : engine.getResults().entrySet()) {
			for (Pair<PortalStudent, Integer> match : entry.getValue()) {
				int count = distanceCounts.computeIfAbsent(match.getRight(), key -> 0);
				distanceCounts.put(match.getRight(), ++count);
				if (match.getRight() > 0 && match.getRight() <= 3) {
					reportMatch(match.getLeft(), entry.getKey(), match.getRight());
				}
			}
		}
		System.out.format("%nDistance histogram%n");
		for (Map.Entry<Integer, Integer> entry : distanceCounts.entrySet()) {
			System.out.format("   %1$2d: %2$6d%n", entry.getKey(), entry.getValue());
		}
	}

	private static void reportMatch(PortalStudent pStudent, ScilympiadStudent sStudent, int distance) {
		System.out.format(
			"Match at distance %1$d:%n   %2$s%n   %3$s%n",
			distance, pStudent, sStudent);
	}
}
