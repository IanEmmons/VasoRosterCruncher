package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ScilympiadStudent implements Comparable<ScilympiadStudent> {
	static enum Column {
		TEAM_NUMBER(0),
		STUDENT_NAME(1),
		GRADE(3);

		public final int columnIndex;

		private Column(int columnIndex) {
			this.columnIndex = columnIndex;
		}
	}

	private static final Pattern ROSTER_FILE_PATTERN = Pattern.compile(
		"scilympiad-.*\\.xlsx");
	static final Pattern SCHOOL_PATTERN = Pattern.compile(
		"^School: (.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TEAM_NAME_PATTERN = Pattern.compile(
		"^Team Name: (.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TEAM_NUMBER_PATTERN = Pattern.compile(
		"^([ABC][0-9]+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern GRADE_PATTERN = Pattern.compile(
		"^([0-9]+)\\s*((st)|(nd)|(rd)|(th))?$", Pattern.CASE_INSENSITIVE);

	public final String school;
	public final String teamName;
	public final String teamNumber;
	public final String firstName;
	public final String lastName;
	public final int grade;

	public static List<ScilympiadStudent> readLatestRosterFile(File rosterDir)
			throws IOException, ParseException {
		File rosterFile;
		try (Stream<Path> stream = Files.find(rosterDir.toPath(), Integer.MAX_VALUE,
				ScilympiadStudent::matcher, FileVisitOption.FOLLOW_LINKS)) {
			rosterFile = stream
				.max(Comparator.comparing(path -> path.getFileName().toString()))
				.map(Path::toFile)
				.orElse(null);
		}

		if (rosterFile == null) {
			return List.of();
		}

		return ScilympiadStudent.parse(rosterFile);
	}

	private static boolean matcher(Path path, BasicFileAttributes attrs) {
		return attrs.isRegularFile()
			&& ROSTER_FILE_PATTERN.matcher(path.getFileName().toString()).matches();
	}

	public static List<ScilympiadStudent> parse(File scilympiadStudentFile)
			throws IOException, ParseException {
		try (InputStream is = new FileInputStream(scilympiadStudentFile)) {
			return parse(is);
		}
	}

	public static List<ScilympiadStudent> parse(String scilympiadStudentResource)
			throws IOException, ParseException {
		try (InputStream is = Util.getResourceAsInputStream(scilympiadStudentResource)) {
			return parse(is);
		}
	}

	public static List<ScilympiadStudent> parse(InputStream scilympiadStudentStream)
			throws IOException, ParseException {
		Stopwatch timer = new Stopwatch();
		try (Workbook workbook = new XSSFWorkbook(scilympiadStudentStream)) {
			List<ScilympiadStudent> result = new ArrayList<>();

			String currentSchool = "";
			String currentTeamName = "";
			Sheet sheet = workbook.getSheetAt(0);

			Iterator<Row> iter = sheet.iterator();
			if (iter.hasNext()) {
				iter.next();	// skip the column headings
			}
			for (int rowNum = 2; iter.hasNext(); ++rowNum) {
				Row row = iter.next();
				String firstColumn = getCellValue(row, Column.TEAM_NUMBER);
				String school = getMatchedPortion(firstColumn, SCHOOL_PATTERN);
				String teamName = getMatchedPortion(firstColumn, TEAM_NAME_PATTERN);
				String teamNumber = getMatchedPortion(firstColumn, TEAM_NUMBER_PATTERN);
				if (firstColumn.isEmpty()) {
					// Do nothing
				} else if (school != null) {
					currentSchool = school;
				} else if (teamName != null) {
					currentTeamName = teamName;
				} else if (teamNumber == null) {
					throw new ParseException("Unexpected value '%1$s' in cell A%2$d",
						firstColumn, rowNum);
				} else {
					result.add(new ScilympiadStudent(currentSchool, currentTeamName,
						teamNumber, getCellValue(row, Column.STUDENT_NAME),
						getCellValue(row, Column.GRADE), rowNum));
				}
			}

			timer.stopAndReport("Parsed Scilympiad student file");
			return result;
		}
	}

	static String getCellValue(Row row, Column column) {
		Cell cell = row.getCell(column.columnIndex, MissingCellPolicy.RETURN_BLANK_AS_NULL);
		if (cell == null) {
			return "";
		} else {
			String content = cell.getStringCellValue();
			return (content == null)
				? ""
				: Util.normalizeSpace(content);
		}
	}

	static String getMatchedPortion(String str, Pattern pattern) {
		Matcher m = pattern.matcher(str);
		return m.matches()
			? m.group(1)
			: null;
	}

	private ScilympiadStudent(String school, String teamName, String teamNumber,
			String fullName, String grade, int rowNum) throws ParseException {
		this.school = School.normalize(school);
		this.teamName = teamName.toLowerCase();
		this.teamNumber = teamNumber.toLowerCase();

		String[] pieces = splitFullName(fullName);
		if (pieces.length == 2) {
			firstName = pieces[1].toLowerCase();
			lastName = pieces[0].toLowerCase();
		} else {
			throw new ParseException("Name '%1$s' in row %2$d is not in last, first format",
				fullName, rowNum);
		}

		if ("K".equalsIgnoreCase(grade)) {
			this.grade = 0;
		} else {
			String gradeNumStr = getMatchedPortion(grade, GRADE_PATTERN);
			if (gradeNumStr == null) {
				throw new ParseException("Grade '%1$s' in row %2$d is malformed", grade, rowNum);
			}
			this.grade = Integer.parseInt(gradeNumStr);
		}
	}

	ScilympiadStudent(String school, String teamName, String teamNumber,
		String lastName, String firstName, int grade, int rowNum) {
		this.school = School.normalize(school);
		this.teamName = Util.normalizeSpace(teamName).toLowerCase();
		this.teamNumber = Util.normalizeSpace(teamNumber).toLowerCase();
		this.lastName = Util.normalizeSpace(lastName).toLowerCase();
		this.firstName = Util.normalizeSpace(firstName).toLowerCase();
		this.grade = grade;
	}

	private static String[] splitFullName(String fullName) {
		String[] pieces = fullName.split(",", 2);
		for (int i = 0; i < pieces.length; ++i) {
			pieces[i] = pieces[i].strip();
		}
		return pieces;
	}

	@Override
	public int hashCode() {
		return Objects.hash(school, lastName, firstName, grade);
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (!(rhs instanceof ScilympiadStudent rhsAsSS)) {
			return false;
		} else {
			return this.compareTo(rhsAsSS) == 0;
		}
	}

	@Override
	public int compareTo(ScilympiadStudent rhs) {
		return new CompareToBuilder()
			.append(this.school, rhs.school)
			.append(this.lastName, rhs.lastName)
			.append(this.firstName, rhs.firstName)
			.append(this.grade, rhs.grade)
			.toComparison();
	}

	@Override
	public String toString() {
		return "ScilympiadStudent [grade=%d, last=%s, first=%s, school=%s]".formatted(
			grade, lastName, firstName, school);
	}
}
