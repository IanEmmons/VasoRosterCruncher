package org.virginiaso.roster_diff;

public record Coach(
	String firstName,
	String lastName,
	String email,
	String phone,
	String school)
{
	public String prettyEmail() {
		return String.format("%1$s %2$s <%3$s>", firstName, lastName, email);
	}

	@Override
	public String toString() {
		return String.format("Coach [%1$s, %2$s, %3$s]", prettyEmail(), phone, school);
	}
}
