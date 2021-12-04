package org.virginiaso.roster_diff;

public record Coach(
	String firstName,
	String lastName,
	String email,
	String school)
{
	public String prettyEmail() {
		return "%1$s %2$s <%3$s>".formatted(firstName, lastName, email);
	}

	@Override
	public String toString() {
		return "Coach [%1$s, %2$s]".formatted(prettyEmail(), school);
	}
}
