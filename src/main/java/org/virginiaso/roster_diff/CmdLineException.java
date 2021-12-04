package org.virginiaso.roster_diff;

public class CmdLineException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmdLineException(String formatStr, Object... args) {
		super(formatStr.formatted(args));
	}

	public CmdLineException(Throwable cause, String formatStr, Object... args) {
		super(formatStr.formatted(args), cause);
	}
}
