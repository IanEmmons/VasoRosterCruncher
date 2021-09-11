package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Match {
	private static enum Column {
		SOURCE(0),
		DISTANCE(1),
		SCHOOL(2),
		TEAM_NAME(3),
		TEAM_NUMBER(4),
		LAST_NAME(5),
		FIRST_NAME(6),
		NICKNAME(7),
		GRADE(8),
		VERDICT(9);

		public final int columnIndex;

		private Column(int columnIndex) {
			this.columnIndex = columnIndex;
		}
	}

	private final ScilympiadStudent sStudent;
	private final PortalStudent pStudent;
	private final Verdict verdict;

	Match(ScilympiadStudent sStudent, PortalStudent pStudent, Verdict verdict) {
		this.sStudent = sStudent;
		this.pStudent = pStudent;
		this.verdict = verdict;
	}

	public static List<Match> parse(File masterReportFile)
			throws IOException, ParseException {
		if (!masterReportFile.isFile()) {
			return new ArrayList<>();
		}
		try (InputStream is = new FileInputStream(masterReportFile)) {
			return parse(is);
		}
	}

	public static List<Match> parse(String masterReportResource)
			throws IOException, ParseException {
		try (InputStream is = Util.getResourceAsInputStream(masterReportResource)) {
			return parse(is);
		}
	}

	public static List<Match> parse(InputStream masterReportStream)
			throws IOException, ParseException {
		Stopwatch timer = new Stopwatch();
		try (Workbook workbook = new XSSFWorkbook(masterReportStream)) {
			List<Match> result = new ArrayList<>();

			Sheet sheet = workbook.getSheet(ReportBuilder.MATCHES_SHEET_TITLE);
			Iterator<Row> iter = sheet.iterator();
			if (iter.hasNext()) {
				iter.next();	// skip the column headings
			}
			ScilympiadStudent currentSStudent = null;
			for (int rowNum = 2; iter.hasNext(); ++rowNum) {
				Row row = iter.next();
				String source = getStringCellValue(row, Column.SOURCE);
				String school = getStringCellValue(row, Column.SCHOOL);
				String teamName = getStringCellValue(row, Column.TEAM_NAME);
				String teamNumber = getStringCellValue(row, Column.TEAM_NUMBER);
				String lastName = getStringCellValue(row, Column.LAST_NAME);
				String firstName = getStringCellValue(row, Column.FIRST_NAME);
				String nickName = getStringCellValue(row, Column.NICKNAME);
				int grade = getNumericCellValue(row, Column.GRADE);
				if (ReportBuilder.SCILYMPIAD_ROW_LABEL.equalsIgnoreCase(source)) {
					currentSStudent = new ScilympiadStudent(school, teamName, teamNumber,
						lastName, firstName, grade, rowNum);
				} else if (ReportBuilder.PORTAL_ROW_LABEL.equalsIgnoreCase(source)) {
					PortalStudent pStudent = new PortalStudent(firstName, lastName, nickName,
						school, grade);
					Verdict verdict = Verdict.fromMasterReport(
						getStringCellValue(row, Column.VERDICT));
					if (verdict != null) {
						result.add(new Match(currentSStudent, pStudent, verdict));
					}
				}
			}

			timer.stopAndReport("Parsed master report");
			return result;
		}
	}

	private static String getStringCellValue(Row row, Column column) {
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

	private static int getNumericCellValue(Row row, Column column) {
		Cell cell = row.getCell(column.columnIndex, MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return (cell == null)
			? -1
			: (int) Math.round(cell.getNumericCellValue());
	}

	public ScilympiadStudent getSStudent() {
		return sStudent;
	}

	public PortalStudent getPStudent() {
		return pStudent;
	}

	public Verdict getVerdict() {
		return verdict;
	}
}
