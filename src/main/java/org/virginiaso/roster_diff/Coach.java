package org.virginiaso.roster_diff;

public class Coach {
	public final String firstName;
	public final String lastName;
	public final String email;
	public final String phone;
	public final String school;

	public Coach() {
		this.firstName = null;
		this.lastName = null;
		this.email = null;
		this.phone = null;
		this.school = null;
	}

	public Coach(String firstName, String lastName, String email, String phone, String school) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phone = phone;
		this.school = school;
	}
}
