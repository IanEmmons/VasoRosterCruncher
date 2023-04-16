package org.virginiaso.roster_cruncher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

public class SchoolNameMappingSheet {
	private static class SchoolNameMappingSheetImpl
			extends MasterReportSheet<Pair<String, String>, SchoolNameMappingSheetImpl.Column> {

		public static enum Column implements MasterReportSheet.ColumnEnum {
			SCILYMPIAD_NAME("Scilympiad Name"),
			CANONICAL_NAME("Canonical Name");

			private final String heading;

			private Column(String heading) {
				this.heading = heading;
			}

			@Override
			public String heading() {
				return heading;
			}
		}

		public SchoolNameMappingSheetImpl() {
			super(Column.class);
		}

		@Override
		protected String sheetTitle() {
			return "School Name Mapping";
		}

		@Override
		protected Pair<String, String> parseRow(Row row) {
			return Pair.of(
				getStringCellValue(row, Column.SCILYMPIAD_NAME),
				getStringCellValue(row, Column.CANONICAL_NAME));
		}

		@Override
		protected void writeRow(Row row, Pair<String, String> mapping) {
			createNextCell(row, CellType.STRING)
				.setCellValue(mapping.getLeft());
			createNextCell(row, CellType.STRING)
				.setCellValue(mapping.getRight());
		}

		@Override
		protected boolean createRowFilter(Pair<String, String> rowItem) {
			return !rowItem.getValue().equals(rowItem.getKey());
		}
	}

	private final SchoolNameMappingSheetImpl impl;

	public SchoolNameMappingSheet() {
		impl = new SchoolNameMappingSheetImpl();
	}

	public Map<String, String> parse(File masterReportFile)
			throws IOException, ParseException {
		var mapping = impl.parse(masterReportFile).stream()
			.collect(Collectors.toMap(
				Pair::getKey,		// key mapper
				Pair::getValue,	// value mapper
				(v1, v2) -> { throw new IllegalStateException(
					"One Scilympiad school maps to both '%1$s' and '%2$s'".formatted(v1, v2));
					},					// merge function
				TreeMap::new));	// map factory

		var canonicalSchoolNames = getCanonicalSchoolNames();
		ensureCanonicalNamesAreInPortal(mapping, canonicalSchoolNames);
		addIdentityMappings(mapping, canonicalSchoolNames);

		return Collections.unmodifiableMap(mapping);
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

	public void create(Workbook workbook, Map<String, String> schoolNameMapping) {
		var rowItems = schoolNameMapping.entrySet().stream()
			.map(Pair::of)
			.collect(Collectors.toCollection(TreeSet::new));
		impl.create(workbook, rowItems);
	}
}
