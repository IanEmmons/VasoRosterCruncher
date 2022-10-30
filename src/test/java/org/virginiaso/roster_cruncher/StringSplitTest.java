package org.virginiaso.roster_cruncher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StringSplitTest {
	@ParameterizedTest
	@MethodSource("stringSplitTestData")
	public void stringSplitTest(String[] expectedOutput, String input, int limit) {
		String[] actualOutput = input.split(";", limit);
		assertArrayEquals(expectedOutput, actualOutput);
	}

	private static Stream<Arguments> stringSplitTestData() {
		return Stream.of(
			Arguments.of(new String[] { "foo", "bar" }, "foo;bar", 0),
			Arguments.of(new String[] { "foo" }, "foo", 0),
			Arguments.of(new String[] { "" }, "", 0));
	}
}
