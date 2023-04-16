package org.virginiaso.roster_cruncher;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public class IgnoredCoachSheet extends MasterReportSheet<String, IgnoredCoachSheet.Column> {
	public static enum Column implements MasterReportSheet.ColumnEnum {
		COACH_EMAIL("Coach Email");

		private final String heading;

		private Column(String heading) {
			this.heading = heading;
		}

		@Override
		public String heading() {
			return heading;
		}
	}

	public IgnoredCoachSheet() {
		super(Column.class);
	}

	@Override
	protected String sheetTitle() {
		return "Ignored Coaches";
	}

	@Override
	protected String parseRow(Row row) {
		return getStringCellValue(row, Column.COACH_EMAIL);
	}

	@Override
	protected void writeRow(Row row, String coachEmail) {
		createNextCell(row, CellType.STRING)
			.setCellValue(coachEmail);
	}
}
