package org.virginiaso.roster_cruncher;

import static org.virginiaso.roster_cruncher.MasterReportSheet.autoSizeColumns;
import static org.virginiaso.roster_cruncher.MasterReportSheet.createNextCell;
import static org.virginiaso.roster_cruncher.MasterReportSheet.createNextRow;
import static org.virginiaso.roster_cruncher.MasterReportSheet.setHeadings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
	static final String MATCHES_SHEET_TITLE = "Adjudicated Matches";
	private static final String[] MATCHES_SHEET_HEADINGS = { "Source", "Distance",
		"School", "Last Name", "First Name", "Nickname", "Grade", "Verdict"
	};
	static final String SCILYMPIAD_ROW_LABEL = "Scilympiad:";
	static final String PORTAL_ROW_LABEL = "Portal:";
	private static final int VERDICT_COLUMN_NUMBER = 7;
	private static final String[] VERDICT_COLUMN_VALUES = {"—", "Different", "Same"};
	private static final String SCILYMPIAD_STUDENT_ROW_FORMAT = """
				<tr>
					<td>%1$s</td>
					<td>%2$s</td>
					<td>%3$d</td>
				</tr>
		""";
	private static final String PORTAL_STUDENT_ROW_FORMAT = """
				<tr>
					<td>%1$s</td>
					<td>%2$s</td>
					<td>%3$s</td>
					<td>%4$d</td>
				</tr>
		""";

	private static enum Style {
		WHITE, GRAY,
		WHITE_FIRST_COLUMN, GRAY_FIRST_COLUMN,
		WHITE_VERDICT_COLUMN, GRAY_VERDICT_COLUMN
	}

	private final DifferenceEngine engine;
	private final Set<Student> sStudents;
	private final Set<Coach> extraCoaches;
	private final Set<String> ignoredCoaches;
	private final File masterReport;
	private final File reportDir;

	public ReportBuilder(DifferenceEngine engine, Set<Student> sStudents,
			Set<Coach> extraCoaches, Set<String> ignoredCoaches, File masterReport,
			File reportDir) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.sStudents = sStudents;
		this.extraCoaches = extraCoaches;
		this.ignoredCoaches = ignoredCoaches;
		this.masterReport = Objects.requireNonNull(masterReport, "masterReport");
		this.reportDir = Objects.requireNonNull(reportDir, "reportDir");
	}

	public void createMasterReport(Map<String, String> schoolNameMapping, Set<Student> ignoredSStudents) {
		try (Workbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX)) {
			createMatchesSheet(workbook);
			new SchoolNameMappingSheet().create(workbook, schoolNameMapping);
			new ExtraCoachSheet().create(workbook, extraCoaches);
			new IgnoredCoachSheet().create(workbook, ignoredCoaches);
			new IgnoredScilympiadStudentSheet().create(workbook, ignoredSStudents);
			new SNotInPSheet().create(workbook, engine.getSStudentsNotFoundInP());
			new PNotInSSheet().create(workbook, engine.getPStudentsNotFoundInS());

			try (OutputStream os = new FileOutputStream(getReportFile(null))) {
				workbook.write(os);
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public void createSchoolReport(String schoolName, List<Coach> coaches, boolean sendEmail) {
		var numSStudentsNotFoundInP = engine.getSStudentsNotFoundInP().stream()
			.filter(student -> student.school().equals(schoolName))
			.count();
		if (numSStudentsNotFoundInP <= 0) {
			var numSStudentsOnSchoolTeams = sStudents.stream()
				.filter(student -> student.school().equals(schoolName))
				.count();
			if (numSStudentsOnSchoolTeams > 0) {
				System.out.format("No missing permissions: %1$s%n", schoolName);
			} else {
				System.out.format("Teams not set up in Scilympiad yet: %1$s%n", schoolName);
			}
			return;
		}

		var emailBody = createSchoolReport(schoolName);
		if (sendEmail) {
			var emailSubject = Config.inst().getMailSubject().formatted(schoolName);
			var recipients = coaches.stream()
				.filter(coach -> !ignoredCoaches.contains(coach.email()))
				.map(Coach::prettyEmail)
				.collect(Collectors.toUnmodifiableList());
			Emailer.send(emailSubject, emailBody, null, schoolName, recipients);
		}
	}

	private void createMatchesSheet(Workbook workbook) {
		/*
		 * First, we create a new matches data structure that combines the near-matches
		 * found by the difference engine with the manually adjudicated matches from the
		 * master report. The data structure here is a map of Scilympiad students to a
		 * map of distance to list of Portal students.
		 */
		Map<Student, Map<Integer, List<Student>>> matchesForDisplay = new TreeMap<>();
		matchesForDisplay.putAll(engine.getResults());
		engine.getMatches().stream()
			.filter(match -> match.getVerdict() != Verdict.EXACT_MATCH)
			.forEach(match -> matchesForDisplay
				.computeIfAbsent(match.getSStudent(), key -> new TreeMap<>())
				.computeIfAbsent(match.getVerdict().getCorrespondingDistance(), key -> new ArrayList<>())
				.add(match.getPStudent()));

		EnumMap<Style, CellStyle> styles = createMatchesSheetStyles(workbook);

		var sheet = workbook.createSheet(MATCHES_SHEET_TITLE);
		setHeadings(sheet, List.of(MATCHES_SHEET_HEADINGS));
		boolean isEvenSStudentIndex = false;
		List<Integer> portalRowNumbers = new ArrayList<>();
		for (var entry : matchesForDisplay.entrySet()) {
			Student sStudent = entry.getKey();
			Map<Integer, List<Student>> matches = entry.getValue();
			createNearMatchRowScilympiad(sheet, sStudent, matches, styles,
				isEvenSStudentIndex, portalRowNumbers);
			isEvenSStudentIndex = !isEvenSStudentIndex;
		}
		setValidation(sheet, portalRowNumbers);
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	private static EnumMap<Style, CellStyle> createMatchesSheetStyles(Workbook workbook) {
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

	private static void createNearMatchRowScilympiad(Sheet sheet, Student sStudent,
			Map<Integer, List<Student>> matches, EnumMap<Style, CellStyle> styles,
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
			.setCellValue(sStudent.school());
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(sStudent.lastName());
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(sStudent.firstName());
		createNextCell(row, CellType.BLANK, subsequentStyle);
		createNextCell(row, CellType.NUMERIC, subsequentStyle)
			.setCellValue(sStudent.grade());
		createNextCell(row, CellType.BLANK, verdictStyle);

		matches.entrySet().stream().forEach(
			entry -> entry.getValue().forEach(
				pStudent -> createNearMatchRowPortal(sheet, entry.getKey(), pStudent,
					firstStyle, subsequentStyle, portalRowNumbers)));
	}

	private static void createNearMatchRowPortal(Sheet sheet, int distance,
			Student pStudent, CellStyle firstStyle, CellStyle subsequentStyle,
			List<Integer> portalRowNumbers) {
		Row row = createNextRow(sheet);
		portalRowNumbers.add(row.getRowNum());
		createNextCell(row, CellType.STRING, firstStyle)
			.setCellValue(PORTAL_ROW_LABEL);
		if (distance < 0) {
			createNextCell(row, CellType.BLANK, subsequentStyle);
		} else {
			createNextCell(row, CellType.NUMERIC, subsequentStyle)
				.setCellValue(distance);
		}
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.school());
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.lastName());
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.firstName());
		createNextCell(row, CellType.STRING, subsequentStyle)
			.setCellValue(pStudent.nickName());
		createNextCell(row, CellType.NUMERIC, subsequentStyle)
			.setCellValue(pStudent.grade());
		if (distance == Verdict.SAME.getCorrespondingDistance()) {
			createNextCell(row, CellType.STRING, subsequentStyle)
				.setCellValue(VERDICT_COLUMN_VALUES[2]);
		} else if (distance == Verdict.DIFFERENT.getCorrespondingDistance()) {
			createNextCell(row, CellType.STRING, subsequentStyle)
				.setCellValue(VERDICT_COLUMN_VALUES[1]);
		} else {
			createNextCell(row, CellType.STRING, subsequentStyle)
				.setCellValue(VERDICT_COLUMN_VALUES[0]);
		}
	}

	private static void setValidation(Sheet sheet, List<Integer> portalRowNumbers) {
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

	private String createSchoolReport(String schoolName) {
		var sStudentsNotInP = engine.getSStudentsNotFoundInP().stream()
			.filter(student -> student.school().equals(schoolName))
			.map(student -> SCILYMPIAD_STUDENT_ROW_FORMAT.formatted(
				student.lastName(), student.firstName(), student.grade()))
			.collect(Collectors.joining());
		var pStudentsNotInS = engine.getPStudentsNotFoundInS().stream()
			.filter(student -> student.school().equals(schoolName))
			.map(student -> PORTAL_STUDENT_ROW_FORMAT.formatted(
				student.lastName(), student.firstName(), student.nickName(), student.grade()))
			.collect(Collectors.joining());

		try {
			var emailBodyResourceName = Config.inst().getMailBodyResourceName();
			var permissionUrl = Config.inst().getPortalPermissionUrl();
			var mtgSummonsUrl = Config.inst().getMailMtgSummonsUrl();
			var emailBody = Util.getResourceAsString(emailBodyResourceName)
				.formatted(schoolName, permissionUrl, sStudentsNotInP, pStudentsNotInS, mtgSummonsUrl);

			Path file = getReportFile(schoolName).toPath();
			Files.writeString(file, emailBody, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE_NEW);

			return emailBody;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private File getReportFile(String schoolName) {
		if (schoolName == null) {
			return masterReport;
		} else {
			reportDir.mkdirs();
			StringBuilder buffer = new StringBuilder();
			schoolName.chars()
				.filter(ch -> ch != '.')
				.map(ch -> (ch == ' ') ? '-' : ch)
				.forEach(ch -> buffer.append((char) ch));
			return new File(reportDir, "%1$s.html".formatted(buffer));
		}
	}
}
