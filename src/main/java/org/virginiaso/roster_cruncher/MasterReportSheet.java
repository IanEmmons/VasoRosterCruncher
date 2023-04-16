package org.virginiaso.roster_cruncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public abstract class MasterReportSheet<RowItem, E extends Enum<E> & MasterReportSheet.ColumnEnum> {
	public static interface ColumnEnum {
		String heading();
	}

	private final Class<E> enumClass;

	MasterReportSheet(Class<E> enumClass) {
		this.enumClass = enumClass;
	}

	protected abstract String sheetTitle();
	protected abstract RowItem parseRow(Row row);
	protected abstract void writeRow(Row row, RowItem rowItem);

	// ===============   Parsing   ===============

	public Set<RowItem> parse(File masterReportFile) throws IOException, ParseException {
		if (!masterReportFile.isFile()) {
			return Collections.emptySet();
		}
		try (InputStream is = new FileInputStream(masterReportFile)) {
			return parse(is);
		}
	}

	private Set<RowItem> parse(InputStream masterReportStream)
			throws IOException, ParseException {
		Stopwatch timer = new Stopwatch();
		try (Workbook workbook = new XSSFWorkbook(masterReportStream)) {
			var result = Util.asStream(workbook.getSheet(sheetTitle()))
				.skip(1)	// skip column headings
				.map(this::parseRow)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new));
			timer.stopAndReport("Parsed %1$s sheet".formatted(sheetTitle()));
			return Collections.unmodifiableSet(result);
		}
	}

	// ===============   Serializing   ===============

	public void create(Workbook workbook, Set<RowItem> rowItems) {
		var sheet = workbook.createSheet(sheetTitle());
		setHeadings(sheet, getHeadings());
		rowItems.stream()
			.filter(this::createRowFilter)
			.forEach(rowItem -> writeRow(createNextRow(sheet), rowItem));
		sheet.createFreezePane(0, 1);
		autoSizeColumns(sheet);
	}

	protected boolean createRowFilter(RowItem rowItem) {
		return true;
	}

	// ===============   Utility methods   ===============

	public static void setHeadings(Sheet sheet, List<String> headings) {
		Row row = createNextRow(sheet);
		for (String heading : headings) {
			createNextCell(row, CellType.STRING)
				.setCellValue(heading);
		}
	}

	public static Row createNextRow(Sheet sheet) {
		// If the sheet has no rows, getLastRowNum() returns -1:
		return sheet.createRow(sheet.getLastRowNum() + 1);
	}

	public static Cell createNextCell(Row row, CellType cellType, CellStyle cellStyle) {
		Cell cell = createNextCell(row, cellType);
		cell.setCellStyle(cellStyle);
		return cell;
	}

	public static Cell createNextCell(Row row, CellType cellType) {
		// The result from getLastCellNum() already includes the +1:
		int lastCellNum = row.getLastCellNum();
		return row.createCell(
			(lastCellNum == -1) ? 0 : lastCellNum,
			cellType);
	}

	public static void autoSizeColumns(Sheet sheet) {
		for (int colNum = 0; colNum < sheet.getRow(0).getLastCellNum(); ++colNum) {
			sheet.autoSizeColumn(colNum);
		}
	}

	private List<String> getHeadings() {
		try {
			var valuesMethod = enumClass.getDeclaredMethod("values");
			@SuppressWarnings("unchecked")
			var enumValues =  (E[]) valuesMethod.invoke(null);
			return Stream.of(enumValues)
				.map(E::heading)
				.collect(Collectors.toUnmodifiableList());
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Unable to invoke values() method on enumeration %1$s"
				.formatted(enumClass.getName()), ex);
		}
	}

	protected static <E extends Enum<E>> String getStringCellValue(Row row, E column) {
		return getStringCellValue(row, column.ordinal());
	}

	private static String getStringCellValue(Row row, int columnOrdinal) {
		Cell cell = row.getCell(columnOrdinal, MissingCellPolicy.RETURN_BLANK_AS_NULL);
		if (cell == null) {
			return "";
		} else {
			String content = cell.getStringCellValue();
			return (content == null)
				? ""
				: Util.normalizeSpace(content);
		}
	}

	protected static <E extends Enum<E>> int getNumericCellValue(Row row, E column) {
		return getNumericCellValue(row, column.ordinal());
	}

	private static int getNumericCellValue(Row row, int columnOrdinal) {
		Cell cell = row.getCell(columnOrdinal, MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return (cell == null)
			? -1
			: (int) Math.round(cell.getNumericCellValue());
	}
}
