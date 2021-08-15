package org.virginiaso.roster_diff;

public class ParseException extends Exception {
	private static final long serialVersionUID = 1L;

	public ParseException(String format, Object... args) {
		super(String.format(format, args));
	}

	public ParseException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}
}
