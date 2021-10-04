package org.virginiaso.roster_diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.regex.Pattern;

public class Util {
	public static final Charset CHARSET = StandardCharsets.UTF_8;
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

	public static String getResourceAsString(String resourceName) throws IOException {
		try (InputStream is = Util.getResourceAsInputStream(resourceName)) {
			return new String(is.readAllBytes(), CHARSET);
		}
	}

	public static Properties loadPropertiesFromResource(String resourceName) {
		try (
			InputStream is = Util.getResourceAsInputStream(resourceName);
			Reader rdr = new InputStreamReader(is, CHARSET);
		) {
			Properties props = new Properties();
			props.load(rdr);
			return props;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
