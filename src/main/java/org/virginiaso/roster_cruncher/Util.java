package org.virginiaso.roster_cruncher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;

public class Util {
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	public static final String JSON_MEDIA_TYPE = "application/json";
	public static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
		.setHeader()
		.setIgnoreEmptyLines(true)
		.setTrim(true)
		.build();
	private static final Pattern WHITESPACE = Pattern.compile("\\s\\s+");

	private Util() {}	// prevent instantiation

	public static String normalizeSpace(String str) {
		return (str == null)
			? null
			: WHITESPACE.matcher(str.strip()).replaceAll(" ");
	}

	public static File appendToStem(File file, String stemSuffix) {
		return new File(file.getParentFile(), "%1$s%2$s.%3$s".formatted(
			getStem(file), stemSuffix, getExt(file)));
	}

	public static File changeExt(File file, String newExt) {
		return new File(file.getParentFile(), "%1$s.%2$s".formatted(getStem(file), newExt));
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

	public static <T> Set<T> setDiff(Set<T> lhs, Set<T> rhs) {
		Set<T> diff = new TreeSet<>(lhs);
		diff.removeAll(rhs);
		return diff;
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

	public static <T> Stream<T> asStream(Iterable<T> it) {
		return StreamSupport.stream(it.spliterator(), false);
	}

	public static <T> Stream<T> asStream(Iterator<T> it) {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(it, Spliterator.IMMUTABLE), false);
	}

	public static File parseFileArgument(Properties props, String propName) {
		String fileNameSetting = props.getProperty(propName, "").strip();
		if (fileNameSetting.isEmpty()) {
			throw new IllegalArgumentException(
				"Configuration setting '%1$s' is missing".formatted(propName));
		}
		return new File(fileNameSetting);
	}

	public static String getStringCellValue(Row row, int columnOrdinal) {
		Cell cell = row.getCell(columnOrdinal, MissingCellPolicy.RETURN_BLANK_AS_NULL);
		if (cell == null) {
			return "";
		} else {
			String content = cell.getStringCellValue();
			return (content == null)
				? ""
				: Util.normalizeSpace(content);
		}
	}

	public static int getNumericCellValue(Row row, int columnOrdinal) {
		Cell cell = row.getCell(columnOrdinal, MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return (cell == null)
			? -1
			: (int) Math.round(cell.getNumericCellValue());
	}
}
