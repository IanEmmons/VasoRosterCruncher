package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

public class ReportBuilder {
	private final DifferenceEngine engine;
	private final File reportDir;
	private final String school;

	public static void newReport(DifferenceEngine engine, File reportDir, String school)
			throws InvalidFormatException, IOException {
		ReportBuilder builder = new ReportBuilder(engine, reportDir, school);
		builder.createReport();
	}

	private ReportBuilder(DifferenceEngine engine, File reportDir, String school) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.reportDir = Objects.requireNonNull(reportDir, "reportDir");
		this.school = (school == null || school.trim().isEmpty())
			? null
			: School.normalize(school);
	}

	private void createReport() throws InvalidFormatException, IOException {
		Stopwatch timer = new Stopwatch();

		try (XSSFWorkbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX)) {
			createMatchesSheet(workbook);
			createSNotInPSheet(workbook);
			createPNotInSSheet(workbook);

			reportDir.mkdirs();
			File reportFile = new File(reportDir,
				String.format("%1$s.xlsx", (school == null) ? "all" : school));
			try (OutputStream os = new FileOutputStream(reportFile)) {
				workbook.write(os);
			}
		}

		timer.stopAndReport("Built report for %1$s", school);
	}

	private void createMatchesSheet(XSSFWorkbook workbook) {
		XSSFSheet sheet = workbook.createSheet("Near Matches");
		setHeadings(sheet, "Source", "Distance", "School", "Team Name", "Team Number",
			"Last Name", "First Name", "Nickname", "Grade");
		engine.getResults().entrySet().stream()
			.filter(entry -> (school == null || entry.getKey().school.equals(school)))
			.forEach(entry -> createNearMatchRowScilympiad(sheet, entry.getKey(), entry.getValue()));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private void createNearMatchRowScilympiad(XSSFSheet sheet, ScilympiadStudent sStudent,
			Map<Integer, List<PortalStudent>> matches) {
		XSSFRow row = createNextRow(sheet);
		createNextCell(row, CellType.STRING)
			.setCellValue("Scilympiad");
		createNextCell(row, CellType.BLANK);
		createNextCell(row, CellType.STRING)
			.setCellValue(sStudent.school);
		createNextCell(row, CellType.STRING)
			.setCellValue(sStudent.teamName);
		createNextCell(row, CellType.STRING)
			.setCellValue(sStudent.teamNumber.toUpperCase());
		createNextCell(row, CellType.STRING)
			.setCellValue(sStudent.lastName);
		createNextCell(row, CellType.STRING)
			.setCellValue(sStudent.firstName);
		createNextCell(row, CellType.BLANK);
		createNextCell(row, CellType.NUMERIC)
			.setCellValue(sStudent.grade);

		matches.entrySet().stream().forEach(
			entry -> entry.getValue().forEach(
				pStudent -> createNearMatchRowPortal(sheet, entry.getKey(), pStudent)));
	}

	private void createNearMatchRowPortal(XSSFSheet sheet, int distance, PortalStudent pStudent) {
		XSSFRow row = createNextRow(sheet);
		createNextCell(row, CellType.STRING)
			.setCellValue("        Portal");
		createNextCell(row, CellType.NUMERIC)
			.setCellValue(distance);
		createNextCell(row, CellType.STRING)
			.setCellValue(pStudent.school);
		createNextCell(row, CellType.BLANK);
		createNextCell(row, CellType.BLANK);
		createNextCell(row, CellType.STRING)
			.setCellValue(pStudent.lastName);
		createNextCell(row, CellType.STRING)
			.setCellValue(pStudent.firstName);
		createNextCell(row, CellType.STRING)
			.setCellValue(pStudent.nickName);
		createNextCell(row, CellType.NUMERIC)
			.setCellValue(pStudent.grade);
	}

	private void createSNotInPSheet(XSSFWorkbook workbook) {
		XSSFSheet sheet = workbook.createSheet("In Scilympiad, not Portal");
		setHeadings(sheet, "School", "Team Name", "Team Number", "Last Name",
			"First Name", "Nickname", "Grade");
		engine.getSStudentsNotFoundInP().stream()
			.filter(student -> (school == null || student.school.equals(school)))
			.forEach(student -> createStudentRow(sheet, student));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private void createStudentRow(XSSFSheet sheet, ScilympiadStudent student) {
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
		createNextCell(row, CellType.BLANK);
		createNextCell(row, CellType.NUMERIC)
			.setCellValue(student.grade);
	}

	private void createPNotInSSheet(XSSFWorkbook workbook) {
		XSSFSheet sheet = workbook.createSheet("In Portal, not Scilympiad");
		setHeadings(sheet, "School", "Team Name", "Team Number", "Last Name",
			"First Name", "Nickname", "Grade");
		engine.getPStudentsNotFoundInS().stream()
			.filter(student -> (school == null || student.school.equals(school)))
			.forEach(student -> createStudentRow(sheet, student));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private void createStudentRow(XSSFSheet sheet, PortalStudent student) {
		XSSFRow row = createNextRow(sheet);
		createNextCell(row, CellType.STRING)
			.setCellValue(student.school);
		createNextCell(row, CellType.BLANK);
		createNextCell(row, CellType.BLANK);
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
