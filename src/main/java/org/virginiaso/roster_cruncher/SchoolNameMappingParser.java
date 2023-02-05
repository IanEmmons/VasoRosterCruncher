package org.virginiaso.roster_cruncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SchoolNameMappingParser {
	private static enum Column {
		SCILYMPIAD_NAME,
		CANONICAL_NAME
	}

	private SchoolNameMappingParser() {}	// prevent instantiation

	public static Map<String, String> parse(File masterReportFile)
			throws IOException, ParseException {
		Map<String, String> mapping = parseWorksheet(masterReportFile);

		var canonicalSchoolNames = getCanonicalSchoolNames();
		ensureCanonicalNamesAreInPortal(mapping, canonicalSchoolNames);
		addIdentityMappings(mapping, canonicalSchoolNames);

		return Collections.unmodifiableMap(mapping);
	}

	public static Map<String, String> parseWorksheet(File masterReportFile)
			throws IOException, ParseException {
		if (!masterReportFile.isFile()) {
			return Collections.emptyMap();
		}
		try (InputStream is = new FileInputStream(masterReportFile)) {
			return parseWorksheet(is);
		}
	}

	private static Map<String, String> parseWorksheet(InputStream masterReportStream)
			throws IOException, ParseException {
		Stopwatch timer = new Stopwatch();
		try (Workbook workbook = new XSSFWorkbook(masterReportStream)) {
			var result = Util.asStream(workbook.getSheet(ReportBuilder.SCHOOL_NAME_SHEET_TITLE))
				.skip(1)	// skip column headings
				.map(row -> Pair.of(
					getStringCellValue(row, Column.SCILYMPIAD_NAME),
					getStringCellValue(row, Column.CANONICAL_NAME)))
				.collect(Collectors.toMap(
					Pair::getKey,		// key mapper
					Pair::getValue,	// value mapper
					(v1, v2) -> { throw new IllegalStateException(
						"One Scilympiad school maps to both '%1$s' and '%2$s'".formatted(v1, v2));
						},					// merge function
					TreeMap::new));	// map factory

			timer.stopAndReport("Parsed school name mapping");
			return result;
		}
	}

	private static String getStringCellValue(Row row, Column column) {
		return Util.getStringCellValue(row, column.ordinal());
	}


	private static Set<String> getCanonicalSchoolNames() throws IOException {
		var reportDir = Config.inst().getPortalReportDir();
		if (!reportDir.isDirectory()) {
			return Collections.emptySet();
		}

		try (var stream = Files.find(reportDir.toPath(), Integer.MAX_VALUE,
			(path, attrs) -> attrs.isRegularFile(), FileVisitOption.FOLLOW_LINKS)) {
			if (stream.findAny().isEmpty()) {
				return Collections.emptySet();
			}
		}

		return CoachRetrieverFactory.create().readLatestReportFile().stream()
			.map(Coach::school)
			.collect(Collectors.toUnmodifiableSet());
	}

	private static void ensureCanonicalNamesAreInPortal(Map<String, String> mapping,
		Set<String> canonicalSchoolNames) {

		var errMsg = mapping.values().stream()
			.filter(school -> !canonicalSchoolNames.contains(school))
			.sorted()
			.collect(Collectors.joining("%n   ".formatted()));
		if (!errMsg.isEmpty()) {
			throw new SchoolNameException(
				"These canonical school names are not present in the portal:%n   %1$s",
				errMsg);
		}
	}

	private static void addIdentityMappings(Map<String, String> mapping,
		Set<String> canonicalSchoolNames) {

		canonicalSchoolNames.stream()
			.forEach(school -> mapping.put(school, school));
	}
}
