package org.virginiaso.roster_cruncher;

import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public abstract class StudentSheet extends MasterReportSheet<Student, StudentSheet.Column> {
	public static enum Column implements MasterReportSheet.ColumnEnum {
		SCHOOL("School"),
		LAST_NAME("Last Name"),
		FIRST_NAME("First Name"),
		NICKNAME("Nickname"),
		GRADE("Grade");

		private final String heading;

		private Column(String heading) {
			this.heading = heading;
		}

		@Override
		public String heading() {
			return heading;
		}
	}

	public StudentSheet() {
		super(Column.class);
	}

	@Override
	protected abstract String sheetTitle();

	@Override
	protected Student parseRow(Row row) {
		return new Student(
			getStringCellValue(row, Column.FIRST_NAME),
			getStringCellValue(row, Column.LAST_NAME),
			getStringCellValue(row, Column.NICKNAME),
			getStringCellValue(row, Column.SCHOOL),
			getNumericCellValue(row, Column.GRADE));
	}

	@Override
	protected void writeRow(Row row, Student student) {
		createNextCell(row, CellType.STRING)
			.setCellValue(student.school());
		createNextCell(row, CellType.STRING)
			.setCellValue(student.lastName());
		createNextCell(row, CellType.STRING)
			.setCellValue(student.firstName());
		if (Strings.isBlank(student.nickName())) {
			createNextCell(row, CellType.BLANK);
		} else {
			createNextCell(row, CellType.STRING)
				.setCellValue(student.nickName());
		}
		createNextCell(row, CellType.NUMERIC)
			.setCellValue(student.grade());
	}
}
