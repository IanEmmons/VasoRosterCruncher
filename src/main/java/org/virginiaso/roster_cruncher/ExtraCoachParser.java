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

public class ExtraCoachParser {
	private static enum Column {
		SCHOOL,
		LAST_NAME,
		FIRST_NAME,
		EMAIL
	}

	private ExtraCoachParser() {}	// prevent instantiation

	public static Set<Coach> parse(File masterReportFile)
			throws IOException, ParseException {
		if (!masterReportFile.isFile()) {
			return Collections.emptySet();
		}
		try (InputStream is = new FileInputStream(masterReportFile)) {
			return parse(is);
		}
	}

	public static Set<Coach> parse(InputStream masterReportStream)
			throws IOException, ParseException {
		Stopwatch timer = new Stopwatch();
		try (Workbook workbook = new XSSFWorkbook(masterReportStream)) {
			var result = Util.asStream(workbook.getSheet(ReportBuilder.EXTRA_COACH_SHEET_TITLE))
				.skip(1)	// skip column headings
				.map(row -> new Coach(
					getStringCellValue(row, Column.FIRST_NAME),
					getStringCellValue(row, Column.LAST_NAME),
					getStringCellValue(row, Column.EMAIL),
					getStringCellValue(row, Column.SCHOOL)))
				.collect(Collectors.toCollection(TreeSet::new));

			timer.stopAndReport("Parsed extra coaches");
			return Collections.unmodifiableSet(result);
		}
	}

	private static String getStringCellValue(Row row, Column column) {
		return Util.getStringCellValue(row, column.ordinal());
	}
}
