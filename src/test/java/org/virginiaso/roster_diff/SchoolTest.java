package org.virginiaso.roster_diff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

public class SchoolTest {
	private static final String P_RESOURCE = "portal-students-c.csv";
	private static final String S_RESOURCE = "scilympiad-students-c.xlsx";

	@Test
	public void test() throws IOException, ParseException {
		Set<String> rawSchools = new TreeSet<>();

		try (
			InputStream is = Util.getResourceAsInputStream(P_RESOURCE);
			CSVParser parser = CSVParser.parse(is, PortalStudent.CHARSET, PortalStudent.FORMAT);
		) {
			parser.stream()
				.map(record -> record.get("School"))
				.forEach(school -> rawSchools.add(school));
		}

		try (
			InputStream is = Util.getResourceAsInputStream(S_RESOURCE);
			XSSFWorkbook workbook = new XSSFWorkbook(is);
		) {
			Iterator<Row> iter = workbook.getSheetAt(0).iterator();
			if (iter.hasNext()) {
				iter.next();	// skip the column headings
			}
			while (iter.hasNext()) {
				Row row = iter.next();
				String firstColumn = ScilympiadStudent.getCellValue(row,
					ScilympiadStudent.Column.TEAM_NUMBER);
				String school = ScilympiadStudent.getMatchedPortion(firstColumn,
					ScilympiadStudent.SCHOOL_PATTERN);
				if (!firstColumn.isEmpty() && school != null) {
					rawSchools.add(school);
				}
			}
		}

		Map<String, Set<String>> normalizedSchools = rawSchools.stream()
			.collect(Collectors.groupingBy(
				School::normalize,					// classifier
				TreeMap::new,										// mapFactory
				Collectors.toCollection(TreeSet::new)));	// downstream collector

		System.out.format("Normalized schools (%1$d):%n", normalizedSchools.keySet().size());
		normalizedSchools.entrySet().stream()
			.map(entry -> String.format("   %1$s: %2$s%n",
				entry.getKey(),
				entry.getValue().stream().collect(Collectors.joining("', '", "'", "'"))))
			.forEach(line -> System.out.print(line));

		assertEquals(47, normalizedSchools.keySet().size());
	}
}
