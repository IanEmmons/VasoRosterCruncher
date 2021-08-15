package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
		List<PortalStudent> portalStudents = PortalStudent.parse(portalFile);
		List<ScilympiadStudent> scilympiadStudents = ScilympiadStudent.parse(scilympiadFile);

		System.out.format("Found %1$d portal students and %2$d Scilimpiad students%n",
			portalStudents.size(), scilympiadStudents.size());
	}
}
