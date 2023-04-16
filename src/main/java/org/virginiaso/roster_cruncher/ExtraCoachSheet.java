package org.virginiaso.roster_cruncher;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public class ExtraCoachSheet extends MasterReportSheet<Coach, ExtraCoachSheet.Column> {
	public static enum Column implements MasterReportSheet.ColumnEnum {
		SCHOOL("School"),
		LAST_NAME("Last Name"),
		FIRST_NAME("First Name"),
		EMAIL("Email");

		private final String heading;

		private Column(String heading) {
			this.heading = heading;
		}

		@Override
		public String heading() {
			return heading;
		}
	}

	public ExtraCoachSheet() {
		super(Column.class);
	}

	@Override
	protected String sheetTitle() {
		return "Extra Coaches";
	}

	@Override
	protected Coach parseRow(Row row) {
		return new Coach(
			getStringCellValue(row, Column.FIRST_NAME),
			getStringCellValue(row, Column.LAST_NAME),
			getStringCellValue(row, Column.EMAIL),
			getStringCellValue(row, Column.SCHOOL));
	}

	@Override
	protected void writeRow(Row row, Coach coach) {
		createNextCell(row, CellType.STRING)
			.setCellValue(coach.school());
		createNextCell(row, CellType.STRING)
			.setCellValue(coach.lastName());
		createNextCell(row, CellType.STRING)
			.setCellValue(coach.firstName());
		createNextCell(row, CellType.STRING)
			.setCellValue(coach.email());
	}
}
