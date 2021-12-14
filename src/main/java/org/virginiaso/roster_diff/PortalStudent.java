package org.virginiaso.roster_diff;

import java.util.Objects;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class PortalStudent implements Comparable<PortalStudent> {
	public final String firstName;
	public final String lastName;
	public final String nickName;
	public final String school;
	public final int grade;

	PortalStudent(String firstName, String lastName, String nickName, String school, int grade) {
		this.firstName = Util.normalizeSpace(firstName).toLowerCase();
		this.lastName = Util.normalizeSpace(lastName).toLowerCase();
		this.nickName = Util.normalizeSpace(nickName).toLowerCase();
		this.school = School.normalize(school);
		this.grade = grade;
	}

	@Override
	public int hashCode() {
		return Objects.hash(school, lastName, firstName, nickName, grade);
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (!(rhs instanceof PortalStudent rhsAsPS)) {
			return false;
		} else {
			return this.compareTo(rhsAsPS) == 0;
		}
	}

	@Override
	public int compareTo(PortalStudent rhs) {
		return new CompareToBuilder()
			.append(this.school, rhs.school)
			.append(this.lastName, rhs.lastName)
			.append(this.firstName, rhs.firstName)
			.append(this.nickName, rhs.nickName)
			.append(this.grade, rhs.grade)
			.toComparison();
	}

	@Override
	public String toString() {
		return "PortalStudent [grade=%d, last=%s, first=%s, nick=%s, school=%s]".formatted(
			grade, lastName, firstName, nickName, school);
	}
}
