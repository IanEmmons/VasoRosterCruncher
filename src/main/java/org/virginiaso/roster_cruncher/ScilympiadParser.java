package org.virginiaso.roster_cruncher;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ScilympiadParser {
	static enum Column {
		TEAM_NUMBER,
		STUDENT_NAME,
		LOGIN_ID,
		GRADE
	}

	private static class Name {
		public final String last;
		public final String first;

		public Name(String fullName, int rowNum) {
			String[] pieces = fullName.split(",", 2);
			for (int i = 0; i < pieces.length; ++i) {
				pieces[i] = Util.normalizeSpace(pieces[i]);
			}
			if (pieces.length != 2) {
				throw new ParseException("Name '%1$s' in row %2$d is not in last, first format",
					fullName, rowNum);
			}
			last = pieces[0];
			first = pieces[1];
		}
	}

	static final Pattern SCHOOL_PATTERN = Pattern.compile(
		"^School: (.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TEAM_NAME_PATTERN = Pattern.compile(
		"^Team Name: (.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TEAM_NUMBER_PATTERN = Pattern.compile(
		"^((A[0-9]+[a-z]?)|([BC][0-9]+))$", Pattern.CASE_INSENSITIVE);
	private static final Pattern GRADE_PATTERN = Pattern.compile(
		"^([0-9]+)\\s*((st)|(nd)|(rd)|(th))?$", Pattern.CASE_INSENSITIVE);

	private ScilympiadParser() {}	// Prevents Instantiation

	public static Set<Student> readLatestRosterFile(Map<String, String> schoolNameMapping) {
		var timer = new Stopwatch();
		var reportDir = Config.inst().getScilympiadReportDir();
		var result = Stream.of(Config.inst().getScilympiadSuffixes())
			.map(String::strip)
			.map(suffix -> getLatestReportFileForSuffix(reportDir, suffix))
			.filter(Objects::nonNull)
			.map(ScilympiadParser::parse)
			.flatMap(List::stream)
			.collect(Collectors.toUnmodifiableSet());

		var scylimpiadSchools = result.stream()
			.map(Student::school)
			.collect(Collectors.toCollection(TreeSet::new));
		checkForUnmappedScylimpiadSchools(scylimpiadSchools, schoolNameMapping);

		var normalizedResult = result.stream()
			.map(student -> new Student(
				student.firstName(),
				student.lastName(),
				student.nickName(),
				normalize(student.school(), schoolNameMapping),
				student.grade()))
			.filter(student -> StringUtil.isNotBlank(student.school()))
			.collect(Collectors.toUnmodifiableSet());

		timer.stopAndReport("Parsed Scilympiad student files");
		return normalizedResult;
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

	private static List<Student> parse(File scilympiadStudentFile) {
		List<Student> result = new ArrayList<>();
		try (
			InputStream is = new FileInputStream(scilympiadStudentFile);
			Workbook workbook = new XSSFWorkbook(is);
		) {
			var currentSchool = "";
			var rowIter = workbook.getSheetAt(0).iterator();
			if (rowIter.hasNext()) {
				rowIter.next();	// skip the column headings
			}
			for (int rowNum = 2; rowIter.hasNext(); ++rowNum) {
				var row = rowIter.next();
				var firstColumn = getCellValue(row, Column.TEAM_NUMBER);
				var school = getMatchedPortion(firstColumn, SCHOOL_PATTERN);
				var teamName = getMatchedPortion(firstColumn, TEAM_NAME_PATTERN);
				var teamNumber = getMatchedPortion(firstColumn, TEAM_NUMBER_PATTERN);
				if (firstColumn.isEmpty()) {
					// Do nothing
				} else if (school.isPresent()) {
					currentSchool = school.get();
				} else if (teamName.isPresent()) {
					// Do nothing
				} else if (teamNumber.isEmpty()) {
					throw new ParseException("Unexpected value '%1$s' in cell A%2$d",
						firstColumn, rowNum);
				} else {
					var name = new Name(getCellValue(row, Column.STUDENT_NAME), rowNum);
					var grade = parseGrade(getCellValue(row, Column.GRADE), rowNum);
					result.add(new Student(name.first, name.last, "", currentSchool, grade));
				}
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
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

	private static int parseGrade(String gradeStr, int rowNum) {
		return getMatchedPortion(gradeStr, GRADE_PATTERN)
			.map(Integer::parseInt)
			.orElseGet(() -> {
				System.out.format("WARNING: Grade '%1$s' in row %2$d is malformed%n", gradeStr, rowNum);
				return 0;
			});
	}

	static Optional<String> getMatchedPortion(String str, Pattern pattern) {
		Matcher m = pattern.matcher(str);
		return m.matches()
			? Optional.of(m.group(1))
			: Optional.empty();
	}

	private static void checkForUnmappedScylimpiadSchools(Set<String> scylimpiadSchools,
			Map<String, String> schoolNameMapping) {
		var errMsg = scylimpiadSchools.stream()
			.filter(school -> !schoolNameMapping.containsKey(school))
			.collect(Collectors.joining("%n   ".formatted()));
		if (!errMsg.isEmpty()) {
			throw new SchoolNameException(
				"These Scilympid schools have not been mapped to a canonical name:%n   %1$s",
				errMsg);
		}
	}

	private static String normalize(String scilympiadSchoolName,
			Map<String, String> schoolNameMapping) {
		var canonicalName = schoolNameMapping.get(scilympiadSchoolName);
		if (canonicalName == null) {
			throw new SchoolNameException(
				"The Scilympid school name '%1$s' has not been mapped to a canonical name",
				scilympiadSchoolName);
		}
		return canonicalName;
	}
}
