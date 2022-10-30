package org.virginiaso.roster_cruncher;

public class ParseException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ParseException(String formatStr, Object... args) {
		super(formatStr.formatted(args));
	}

	public ParseException(Throwable cause, String formatStr, Object... args) {
		super(formatStr.formatted(args), cause);
	}
}
