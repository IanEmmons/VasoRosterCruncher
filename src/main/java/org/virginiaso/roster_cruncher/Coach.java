package org.virginiaso.roster_cruncher;

import java.util.Objects;

public record Coach(String firstName, String lastName, String email, String school)
		implements Comparable<Coach> {
	public Coach(String fullName, String email, String school) {
		this("", fullName, email, school);
	}

	public String prettyEmail() {
		var name = String.join(" ", firstName, lastName);
		return "%1$s <%2$s>".formatted(name, email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (!(rhs instanceof Coach rhsAsCoach)) {
			return false;
		} else {
			return Objects.equals(email, rhsAsCoach.email);
		}
	}

	@Override
	public int compareTo(Coach rhs) {
		return Objects.compare(this.email(), rhs.email(), String.CASE_INSENSITIVE_ORDER);
	}

	@Override
	public String toString() {
		return "Coach [%1$s, %2$s]".formatted(prettyEmail(), school);
	}
}
