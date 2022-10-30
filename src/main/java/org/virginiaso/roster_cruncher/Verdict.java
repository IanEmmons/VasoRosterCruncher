package org.virginiaso.roster_cruncher;

import java.util.stream.Stream;

public enum Verdict {
	DIFFERENT(-1),
	SAME(-2),
	EXACT_MATCH(0);

	private final int correspondingDistance;

	private Verdict(int correspondingDistance) {
		this.correspondingDistance = correspondingDistance;
	}

	public int getCorrespondingDistance() {
		return correspondingDistance;
	}

	public static Verdict fromMasterReport(String masterReportVerdict) {
		String verdictToSearchFor = (masterReportVerdict == null)
			? null
			: masterReportVerdict.strip().toUpperCase();
		Verdict result = Stream.of(values())
			.filter(value -> value.name().equals(verdictToSearchFor))
			.findFirst()
			.orElse(null);
		if (result == EXACT_MATCH) {
			throw new IllegalStateException(
				"Master report verdicts should not include EXACT_MATCH");
		}
		return result;
	}
}
