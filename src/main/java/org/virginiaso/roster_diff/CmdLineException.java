package org.virginiaso.roster_diff;

public class CmdLineException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmdLineException(String format, Object... args) {
		super(String.format(format, args));
	}

	public CmdLineException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}
}
