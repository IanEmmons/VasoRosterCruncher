package org.virginiaso.roster_diff;

import java.io.File;
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
	public static final String PROPERTIES_RESOURCE = "configuration.properties";
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final Pattern WHITESPACE = Pattern.compile("\\s\\s+");

	private Util() {}	// prevent instantiation

	public static String normalizeSpace(String str) {
		return (str == null)
			? null
			: WHITESPACE.matcher(str.trim()).replaceAll(" ");
	}

	public static File appendToStem(File file, String stemSuffix) {
		return new File(file.getParentFile(), String.format("%1$s%2$s.%3$s",
			getStem(file), stemSuffix, getExt(file)));
	}

	public static File changeExt(File file, String newExt) {
		return new File(file.getParentFile(), String.format("%1$s.%2$s",
			getStem(file), newExt));
	}

	public static String getExt(File file) {
		return getExt(file.getName());
	}

	public static String getExt(String fileName) {
		int i = fileName.lastIndexOf('.');
		return (i == -1) ? "" : fileName.substring(i + 1);
	}

	public static String getStem(File file) {
		return getStem(file.getName());
	}

	public static String getStem(String fileName) {
		int i = fileName.lastIndexOf('.');
		return (i == -1) ? fileName : fileName.substring(0, i);
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
			Properties props = new Properties(System.getProperties());
			props.load(rdr);
			return props;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
