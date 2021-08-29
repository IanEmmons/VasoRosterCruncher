package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class PortalStudent {
	static final Charset CHARSET = StandardCharsets.UTF_8;
	static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
		.setHeader()
		.setIgnoreEmptyLines(true)
		.setTrim(true)
		.build();

	public final String fullName;
	public final String firstName;
	public final String lastName;
	public final String nickName;
	public final String school;
	public final int grade;

	public static List<PortalStudent> parse(File portalStudentFile) throws IOException {
		try (InputStream is = new FileInputStream(portalStudentFile)) {
			return parse(is);
		}
	}

	public static List<PortalStudent> parse(String portalStudentResource) throws IOException {
		try (InputStream is = Util.getResourceAsInputStream(portalStudentResource)) {
			return parse(is);
		}
	}

	public static List<PortalStudent> parse(InputStream portalStudentStream) throws IOException {
		Stopwatch timer = new Stopwatch();
		List<PortalStudent> result;
		try (CSVParser parser = CSVParser.parse(portalStudentStream, CHARSET, FORMAT)) {
			result = parser.stream()
				.map(PortalStudent::new)
				.collect(Collectors.toUnmodifiableList());
		}
		timer.stopAndReport("Parsed portal student file");
		return result;
	}

	private PortalStudent(CSVRecord record) {
		fullName = Util.normalizeSpace(record.get("Student Legal Name"));
		firstName = Util.normalizeSpace(record.get("Student Legal Name: First")).toLowerCase();
		lastName = Util.normalizeSpace(record.get("Student Legal Name: Last")).toLowerCase();
		nickName = Util.normalizeSpace(record.get("Student Nickname")).toLowerCase();
		school = School.normalize(record.get("School"));
		grade = Integer.parseInt(Util.normalizeSpace(record.get("Grade")));
	}

	@Override
	public String toString() {
		return String.format(
			"PortalStudent [grade=%d, last=%s, first=%s, nick=%s, school=%s]",
			grade, lastName, firstName, nickName, school);
	}
}
