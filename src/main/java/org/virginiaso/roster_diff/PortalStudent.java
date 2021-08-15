package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class PortalStudent {
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
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
		try (CSVParser parser = CSVParser.parse(portalStudentFile, CHARSET, FORMAT)) {
			return parser.stream()
				.map(PortalStudent::new)
				.collect(Collectors.toUnmodifiableList());
		}
	}

	private PortalStudent(CSVRecord record) {
		fullName = Util.normalizeSpace(record.get("Student Legal Name"));
		firstName = Util.normalizeSpace(record.get("Student Legal Name: First"));
		lastName = Util.normalizeSpace(record.get("Student Legal Name: Last"));
		nickName = Util.normalizeSpace(record.get("Student Nickname"));
		school = Util.normalizeSpace(record.get("School"));
		grade = Integer.parseInt(Util.normalizeSpace(record.get("Grade")));
	}
}
