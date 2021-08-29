package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

public class ReportBuilder {
	private final DifferenceEngine engine;
	private final String school;
	private final File reportDir;

	public static void newReport(DifferenceEngine engine, String school)
			throws InvalidFormatException, IOException {
		ReportBuilder builder = new ReportBuilder(engine, school);
		builder.createReport();
	}

	private ReportBuilder(DifferenceEngine engine, String school) {
		this.engine = engine;
		this.school = school;
		reportDir = new File(String.format("reports-%1$TF_%1$TT", System.currentTimeMillis()));

		if (reportDir.exists()) {
			throw new IllegalStateException(String.format(
				"Report directory '%1$s' already exists", reportDir.getPath()));
		}
	}

	private void createReport() throws InvalidFormatException, IOException {
		Stopwatch timer = new Stopwatch();

		try (XSSFWorkbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX)) {
			createMatchesSheet(workbook);
			createSNotInPSheet(workbook);
			createPNotInSSheet(workbook);

			reportDir.mkdirs();
			File reportFile = new File(reportDir, String.format("%1$s.xlsx", school));
			try (OutputStream os = new FileOutputStream(reportFile)) {
				workbook.write(os);
			}
		}

		timer.stopAndReport("Built report for %1$s", school);
	}

	private void createMatchesSheet(XSSFWorkbook workbook) {
		XSSFSheet sheet = workbook.createSheet("Near Matches");
		/* S */ setHeadings(sheet, "School", "Team Name", "Team Number", "Last Name",
			"First Name", "Grade");
		/* P */ setHeadings(sheet, "School", "Last Name", "First Name", "Nickname", "Grade");
	}

	private void createSNotInPSheet(XSSFWorkbook workbook) {
		XSSFSheet sheet = workbook.createSheet("In Scilympiad, not Portal");
		setHeadings(sheet, "School", "Team Name", "Team Number", "Last Name",
			"First Name", "Grade");
		engine.getSStudentsNotFoundInP().stream()
			.forEach(student -> setScilympiadStudent(sheet, student));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private void setScilympiadStudent(XSSFSheet sheet, ScilympiadStudent student) {
		XSSFRow row = createNextRow(sheet);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.school);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.teamName);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.teamNumber.toUpperCase());
		createNextCell(row, CellType.STRING)
			.setCellValue(student.lastName);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.firstName);
		createNextCell(row, CellType.NUMERIC)
			.setCellValue(student.grade);
	}

	private void createPNotInSSheet(XSSFWorkbook workbook) {
		XSSFSheet sheet = workbook.createSheet("In Portal, not Scilympiad");
		setHeadings(sheet, "School", "Last Name", "First Name", "Nickname", "Grade");
		engine.getPStudentsNotFoundInS().stream()
			.forEach(student -> setPortalStudent(sheet, student));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private void setPortalStudent(XSSFSheet sheet, PortalStudent student) {
		XSSFRow row = createNextRow(sheet);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.school);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.lastName);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.firstName);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.nickName);
		createNextCell(row, CellType.NUMERIC)
			.setCellValue(student.grade);
	}

	private void setHeadings(XSSFSheet sheet, String... headings) {
		XSSFRow row = createNextRow(sheet);
		for (String heading : headings) {
			createNextCell(row, CellType.STRING)
				.setCellValue(heading);
		}
	}

	private XSSFRow createNextRow(XSSFSheet sheet) {
		// The result from getLastRowNum() does not include the +1:
		int lastRowNum = sheet.getLastRowNum();
		return sheet.createRow(
			(lastRowNum == -1) ? 0 : lastRowNum + 1);
	}

	private XSSFCell createNextCell(XSSFRow row, CellType cellType) {
		// The result from getLastCellNum() already includes the +1:
		int lastCellNum = row.getLastCellNum();
		return row.createCell(
			(lastCellNum == -1) ? 0 : lastCellNum,
			cellType);
	}

	private void autoSizeColumns(XSSFSheet sheet) {
		for (int colNum = 0; colNum < sheet.getRow(0).getLastCellNum(); ++colNum) {
			sheet.autoSizeColumn(colNum);
		}
	}
}
