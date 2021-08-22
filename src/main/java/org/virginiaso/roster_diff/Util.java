package org.virginiaso.roster_diff;

import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.regex.Pattern;

public class Util {
	private static final Pattern WHITESPACE = Pattern.compile("\\s\\s+");

	private Util() {}	// prevent instantiation

	public static String normalizeSpace(String str) {
		return WHITESPACE.matcher(str.trim()).replaceAll(" ");
	}

	public static InputStream getResourceAsInputStream(String resourceName) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream result = cl.getResourceAsStream(resourceName);
		if (result == null) {
			throw new MissingResourceException(null, null, resourceName);
		}
		return result;
	}
}
