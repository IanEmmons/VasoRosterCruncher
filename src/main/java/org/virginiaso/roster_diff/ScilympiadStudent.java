package org.virginiaso.roster_diff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ScilympiadStudent {
	private static enum Column {
		TEAM_NUMBER(0),
		STUDENT_NAME(1),
		GRADE(3);

		public final int columnIndex;

		private Column(int columnIndex) {
			this.columnIndex = columnIndex;
		}
	}

	private static final Pattern SCHOOL_PATTERN = Pattern.compile(
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
	public final String fullName;
	public final String firstName;
	public final String lastName;
	public final int grade;

	public static List<ScilympiadStudent> parse(File scilympiadStudentFile)
			throws IOException, ParseException, InvalidFormatException {
		Stopwatch timer = new Stopwatch();
		try (XSSFWorkbook workbook = new XSSFWorkbook(scilympiadStudentFile)) {
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

	private static String getCellValue(Row row, Column column) {
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

	private static String getMatchedPortion(String str, Pattern pattern) {
		Matcher m = pattern.matcher(str);
		return m.matches()
			? m.group(1)
			: null;
	}

	private ScilympiadStudent(String school, String teamName, String teamNumber,
			String fullName, String grade, int rowNum) throws ParseException {
		this.school = school;
		this.teamName = teamName;
		this.teamNumber = teamNumber;
		this.fullName = fullName;

		String[] pieces = splitFullName(this.fullName);
		if (pieces.length == 2) {
			firstName = pieces[1];
			lastName = pieces[0];
		} else {
			throw new ParseException("Name '%1$s' in row %2$d is not in last, first format",
				this.fullName, rowNum);
		}

		String gradeNumStr = getMatchedPortion(grade, GRADE_PATTERN);
		if (gradeNumStr == null) {
			throw new ParseException("Grade '%1$s' in row %2$d is malformed", grade, rowNum);
		} else {
			this.grade = Integer.parseInt(gradeNumStr);
		}
	}

	private static String[] splitFullName(String fullName) {
		String[] pieces = fullName.split(",", 2);
		for (int i = 0; i < pieces.length; ++i) {
			pieces[i] = pieces[i].trim();
		}
		return pieces;
	}
}
