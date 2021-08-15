package org.virginiaso.roster_diff;

import java.util.regex.Pattern;

public class Util {
	private static final Pattern WHITESPACE = Pattern.compile("\\s\\s+");

	private Util() {}	// prevent instantiation

	public static String normalizeSpace(String str) {
		return WHITESPACE.matcher(str.trim()).replaceAll(" ");
	}
}
