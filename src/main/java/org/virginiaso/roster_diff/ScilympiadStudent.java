package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
		TEAM_NUMBER,
		STUDENT_NAME,
		LOGIN_ID,
		GRADE
	}

	static final Pattern SCHOOL_PATTERN = Pattern.compile(
		"^School: (.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TEAM_NAME_PATTERN = Pattern.compile(
		"^Team Name: (.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TEAM_NUMBER_PATTERN = Pattern.compile(
		"^([ABC][0-9]+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern GRADE_PATTERN = Pattern.compile(
		"^([0-9]+)\\s*((st)|(nd)|(rd)|(th))?$", Pattern.CASE_INSENSITIVE);

	public final String school;
	public final String firstName;
	public final String lastName;
	public final int grade;

	public static List<ScilympiadStudent> readLatestRosterFile()
			throws IOException {
		var props = Util.loadPropertiesFromResource(Util.CONFIGURATION_RESOURCE);
		var siteName = props.getProperty("scilympiad.site");
		var reportDir = Util.parseFileArgument(props, "scilympiad.report.dir");
		var suffixStr = props.getProperty("scilympiad.%1$s.suffixes".formatted(siteName), "");
		return Stream.of(suffixStr.split(","))
			.map(String::strip)
			.map(suffix -> getLatestReportFileForSuffix(reportDir, suffix))
			.filter(file -> file != null)
			.map(ScilympiadStudent::parse)
			.flatMap(List::stream)
			.collect(Collectors.toUnmodifiableList());
	}

	private static File getLatestReportFileForSuffix(File reportDir, String suffix) {
		var fileNamePattern = Pattern.compile("roster%1$s-.*\\.xlsx".formatted(suffix));
		BiPredicate<Path, BasicFileAttributes> matcher = (Path path, BasicFileAttributes attrs)
			-> attrs.isRegularFile() && fileNamePattern.matcher(path.getFileName().toString()).matches();
		try (Stream<Path> stream = Files.find(reportDir.toPath(), Integer.MAX_VALUE,
				matcher, FileVisitOption.FOLLOW_LINKS)) {
			return stream
				.map(Path::toFile)
				.max(Comparator.comparing(File::getName))
				.orElse(null);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static List<ScilympiadStudent> parse(File scilympiadStudentFile) {
		List<ScilympiadStudent> result = new ArrayList<>();
		Stopwatch timer = new Stopwatch();

		try (
			InputStream is = new FileInputStream(scilympiadStudentFile);
			Workbook workbook = new XSSFWorkbook(is);
		) {
			String currentSchool = "";
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
					// Do nothing
				} else if (teamNumber == null) {
					throw new ParseException("Unexpected value '%1$s' in cell A%2$d",
						firstColumn, rowNum);
				} else {
					result.add(new ScilympiadStudent(currentSchool,
						getCellValue(row, Column.STUDENT_NAME),
						getCellValue(row, Column.GRADE), rowNum));
				}
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}

		timer.stopAndReport("Parsed Scilympiad student file");
		return result;
	}

	static String getCellValue(Row row, Column column) {
		Cell cell = row.getCell(column.ordinal(), MissingCellPolicy.RETURN_BLANK_AS_NULL);
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

	private ScilympiadStudent(String school, String fullName, String grade, int rowNum) {
		this.school = School.normalize(school);

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

	ScilympiadStudent(String school, String lastName, String firstName, int grade, int rowNum) {
		this.school = School.normalize(school);
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
