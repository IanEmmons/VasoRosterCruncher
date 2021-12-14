package org.virginiaso.roster_diff;

import java.util.Objects;

import org.apache.commons.lang3.builder.CompareToBuilder;

public record Student(String firstName, String lastName, String nickName, String school,
	int grade) implements Comparable<Student> {

	@Override
	public int hashCode() {
		return Objects.hash(school, lastName, firstName, nickName, grade);
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (!(rhs instanceof Student rhsAsPS)) {
			return false;
		} else {
			return this.compareTo(rhsAsPS) == 0;
		}
	}

	@Override
	public int compareTo(Student rhs) {
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
		return "Student [grade=%d, last=%s, first=%s, nick=%s, school=%s]".formatted(
			grade, lastName, firstName, nickName, school);
	}
}
