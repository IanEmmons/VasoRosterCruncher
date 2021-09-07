package org.virginiaso.roster_diff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

public class ReportBuilder {
	private static final String P_NOT_S_SHEET_TITLE = "In Portal, not Scilympiad";
	private static final String S_NOT_P_SHEET_TITLE = "In Scilympiad, not Portal";
	static final String MATCHES_SHEET_TITLE = "Adjudicated Matches";
	static final String SCILYMPIAD_ROW_LABEL = "Scilympiad:";
	static final String PORTAL_ROW_LABEL = "Portal:";
	private static final int VERDICT_COLUMN_NUMBER = 9;
	private static final String[] VERDICT_COLUMN_VALUES = {"—", "Different", "Same"};

	private static enum Style {
		WHITE, GRAY,
		WHITE_FIRST_COLUMN, GRAY_FIRST_COLUMN,
		WHITE_VERDICT_COLUMN, GRAY_VERDICT_COLUMN
	}

	private final DifferenceEngine engine;
	private final File masterReportFile;
	private final File reportDir;
	private final String school;

	public static void newReport(DifferenceEngine engine, File masterReportFile,
			File reportDir, String school) throws InvalidFormatException, IOException {
		ReportBuilder builder = new ReportBuilder(engine, masterReportFile, reportDir, school);
		builder.createReport();
	}

	private ReportBuilder(DifferenceEngine engine, File masterReportFile, File reportDir,
			String school) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.masterReportFile = Objects.requireNonNull(masterReportFile, "masterReportFile");
		this.reportDir = Objects.requireNonNull(reportDir, "reportDir");
		this.school = (school == null || school.trim().isEmpty())
			? null
			: School.normalize(school);
	}

	private void createReport() throws InvalidFormatException, IOException {
		Stopwatch timer = new Stopwatch();

		try (Workbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX)) {
			if (school == null) {
				createMatchesSheet(workbook);
			}
			createSNotInPSheet(workbook);
			createPNotInSSheet(workbook);

			try (OutputStream os = new FileOutputStream(getReportFile())) {
				workbook.write(os);
			}
		}

		timer.stopAndReport("Built report for %1$s", school);
	}

	private void createMatchesSheet(Workbook workbook) {
		EnumMap<Style, CellStyle> styles = createMatchesSheetStyles(workbook);

		Sheet sheet = workbook.createSheet(MATCHES_SHEET_TITLE);
		setHeadings(sheet, "Source", "Distance", "School", "Team Name", "Team Number",
			"Last Name", "First Name", "Nickname", "Grade", "Verdict");
		boolean isEvenSStudentIndex = false;
		List<Integer> portalRowNumbers = new ArrayList<>();
		for (var entry : engine.getResults().entrySet()) {
			ScilympiadStudent sStudent = entry.getKey();
			Map<Integer, List<PortalStudent>> matches = entry.getValue();
			if (school == null || school.equals(sStudent.school)) {
				createNearMatchRowScilympiad(sheet, sStudent, matches, styles,
					isEvenSStudentIndex, portalRowNumbers);
				isEvenSStudentIndex = !isEvenSStudentIndex;
			}
		}
		setValidation(sheet, portalRowNumbers);
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private EnumMap<Style, CellStyle> createMatchesSheetStyles(Workbook workbook) {
		EnumMap<Style, CellStyle> result = new EnumMap<>(Style.class);

		CellStyle white = workbook.createCellStyle();
		result.put(Style.WHITE, white);

		CellStyle firstColWhite = workbook.createCellStyle();
		firstColWhite.setAlignment(HorizontalAlignment.RIGHT);
		result.put(Style.WHITE_FIRST_COLUMN, firstColWhite);

		CellStyle gray = workbook.createCellStyle();
		gray.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		gray.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		result.put(Style.GRAY, gray);

		CellStyle firstColGray = workbook.createCellStyle();
		firstColGray.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		firstColGray.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		firstColGray.setAlignment(HorizontalAlignment.RIGHT);
		result.put(Style.GRAY_FIRST_COLUMN, firstColGray);

		result.put(Style.WHITE_VERDICT_COLUMN, white);
		result.put(Style.GRAY_VERDICT_COLUMN, gray);

		return result;
	}

	private void createNearMatchRowScilympiad(Sheet sheet, ScilympiadStudent sStudent,
			Map<Integer, List<PortalStudent>> matches, EnumMap<Style, CellStyle> styles,
			boolean isEvenSStudentIndex, List<Integer> portalRowNumbers) {
		Row row = createNextRow(sheet);
		CellStyle firstStyle = styles.get(
			isEvenSStudentIndex ? Style.WHITE_FIRST_COLUMN : Style.GRAY_FIRST_COLUMN);
		CellStyle subsequentStyle = styles.get(
			isEvenSStudentIndex ? Style.WHITE : Style.GRAY);
		CellStyle verdictStyle = styles.get(
			isEvenSStudentIndex ? Style.WHITE_VERDICT_COLUMN : Style.GRAY_VERDICT_COLUMN);
		createNextCell(row, CellType.STRING, firstStyle)
			.setCellValue(SCILYMPIAD_ROW_LABEL);
		createNextCell(row, CellType.BLANK, subsequentStyle);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(sStudent.school);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(sStudent.teamName);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(sStudent.teamNumber.toUpperCase());
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(sStudent.lastName);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(sStudent.firstName);
		createNextCell(row, CellType.BLANK, subsequentStyle);
		createNextCell(row, CellType.NUMERIC, subsequentStyle)
			.setCellValue(sStudent.grade);
		createNextCell(row, CellType.BLANK, verdictStyle);

		matches.entrySet().stream().forEach(
			entry -> entry.getValue().forEach(
				pStudent -> createNearMatchRowPortal(sheet, entry.getKey(), pStudent,
					firstStyle, subsequentStyle, portalRowNumbers)));
	}

	private void createNearMatchRowPortal(Sheet sheet, int distance,
			PortalStudent pStudent, CellStyle firstStyle, CellStyle subsequentStyle,
			List<Integer> portalRowNumbers) {
		Row row = createNextRow(sheet);
		portalRowNumbers.add(row.getRowNum());
		createNextCell(row, CellType.STRING, firstStyle)
			.setCellValue(PORTAL_ROW_LABEL);
		createNextCell(row, CellType.NUMERIC, subsequentStyle)
			.setCellValue(distance);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.school);
		createNextCell(row, CellType.BLANK, subsequentStyle);
		createNextCell(row, CellType.BLANK, subsequentStyle);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.lastName);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.firstName);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.nickName);
		createNextCell(row, CellType.NUMERIC, subsequentStyle)
			.setCellValue(pStudent.grade);
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(VERDICT_COLUMN_VALUES[0]);
	}

	private void setValidation(Sheet sheet, List<Integer> portalRowNumbers) {
		CellRangeAddressList addressList = new CellRangeAddressList();
		for (int rowNumber : portalRowNumbers) {
			addressList.addCellRangeAddress(
				rowNumber, VERDICT_COLUMN_NUMBER, rowNumber, VERDICT_COLUMN_NUMBER);
		}

		DataValidationHelper dvHelper = sheet.getDataValidationHelper();
		DataValidationConstraint dvConstraint
			= dvHelper.createExplicitListConstraint(VERDICT_COLUMN_VALUES);
		DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
		validation.setSuppressDropDownArrow(true);
		validation.setShowErrorBox(true);
		validation.setEmptyCellAllowed(true);
		sheet.addValidationData(validation);
	}

	private void createSNotInPSheet(Workbook workbook) {
		Sheet sheet = workbook.createSheet(S_NOT_P_SHEET_TITLE);
		setHeadings(sheet, "School", "Team Name", "Team Number", "Last Name",
			"First Name", "Nickname", "Grade");
		engine.getSStudentsNotFoundInP().stream()
			.filter(student -> (school == null || student.school.equals(school)))
			.forEach(student -> createStudentRow(sheet, student));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private void createStudentRow(Sheet sheet, ScilympiadStudent student) {
		Row row = createNextRow(sheet);
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

	private void createPNotInSSheet(Workbook workbook) {
		Sheet sheet = workbook.createSheet(P_NOT_S_SHEET_TITLE);
		setHeadings(sheet, "School", "Team Name", "Team Number", "Last Name",
			"First Name", "Nickname", "Grade");
		engine.getPStudentsNotFoundInS().stream()
			.filter(student -> (school == null || student.school.equals(school)))
			.forEach(student -> createStudentRow(sheet, student));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private void createStudentRow(Sheet sheet, PortalStudent student) {
		Row row = createNextRow(sheet);
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

	private void setHeadings(Sheet sheet, String... headings) {
		Row row = createNextRow(sheet);
		for (String heading : headings) {
			createNextCell(row, CellType.STRING)
				.setCellValue(heading);
		}
	}

	private Row createNextRow(Sheet sheet) {
		// The result from getLastRowNum() does not include the +1:
		int lastRowNum = sheet.getLastRowNum();
		return sheet.createRow(
			(lastRowNum == -1) ? 0 : lastRowNum + 1);
	}

	private Cell createNextCell(Row row, CellType cellType, CellStyle cellStyle) {
		Cell cell = createNextCell(row, cellType);
		cell.setCellStyle(cellStyle);
		return cell;
	}

	private Cell createNextCell(Row row, CellType cellType) {
		// The result from getLastCellNum() already includes the +1:
		int lastCellNum = row.getLastCellNum();
		return row.createCell(
			(lastCellNum == -1) ? 0 : lastCellNum,
			cellType);
	}

	private void autoSizeColumns(Sheet sheet) {
		for (int colNum = 0; colNum < sheet.getRow(0).getLastCellNum(); ++colNum) {
			sheet.autoSizeColumn(colNum);
		}
	}

	private File getReportFile() {
		if (school == null) {
			return masterReportFile;
		} else {
			reportDir.mkdirs();
			return new File(reportDir, String.format("%1$s.xlsx", school));
		}
	}
}
