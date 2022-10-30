package org.virginiaso.roster_cruncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class IgnoredScilympiadStudentParser {
	private static enum Column {
		SCHOOL,
		LAST_NAME,
		FIRST_NAME,
		NICKNAME,
		GRADE
	}

	private IgnoredScilympiadStudentParser() {}	// prevent instantiation

	public static Set<Student> parse(File masterReportFile)
			throws IOException, ParseException {
		if (!masterReportFile.isFile()) {
			return Collections.emptySet();
		}
		try (InputStream is = new FileInputStream(masterReportFile)) {
			return parse(is);
		}
	}

	public static Set<Student> parse(InputStream masterReportStream)
			throws IOException, ParseException {
		Stopwatch timer = new Stopwatch();
		try (Workbook workbook = new XSSFWorkbook(masterReportStream)) {
			var result = Util.asStream(workbook.getSheet(ReportBuilder.IGNORED_SHEET_TITLE))
				.skip(1)	// skip column headings
				.map(row -> new Student(
					getStringCellValue(row, Column.FIRST_NAME),
					getStringCellValue(row, Column.LAST_NAME),
					getStringCellValue(row, Column.NICKNAME),
					getStringCellValue(row, Column.SCHOOL),
					getNumericCellValue(row, Column.GRADE)))
				.collect(Collectors.toCollection(TreeSet::new));

			timer.stopAndReport("Parsed master report");
			return Collections.unmodifiableSet(result);
		}
	}

	private static String getStringCellValue(Row row, Column column) {
		return Util.getStringCellValue(row, column.ordinal());
	}

	private static int getNumericCellValue(Row row, Column column) {
		return Util.getNumericCellValue(row, column.ordinal());
	}
}
