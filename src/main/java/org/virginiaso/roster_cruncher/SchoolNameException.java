package org.virginiaso.roster_cruncher;

public class SchoolNameException extends IllegalStateException {
	private static final long serialVersionUID = 1L;

	public SchoolNameException(String formatStr, Object... args) {
		super(formatStr.formatted(args));
	}
}
