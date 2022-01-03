package org.virginiaso.roster_diff;

public record Coach(
	String firstName,
	String lastName,
	String email,
	String school)
{
	public Coach(String fullName, String email, String school) {
		this("", fullName, email, school);
	}

	public String prettyEmail() {
		var name = String.join(" ", firstName, lastName);
		return "%1$s <%2$s>".formatted(name, email);
	}

	@Override
	public String toString() {
		return "Coach [%1$s, %2$s]".formatted(prettyEmail(), school);
	}
}
