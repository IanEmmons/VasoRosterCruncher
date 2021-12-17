package org.virginiaso.roster_diff;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class WeightAvgDistanceFunction implements DistanceFunction {
	private static final int LAST_WEIGHT = 1;		// 2 or 1
	private static final int FIRST_WEIGHT = 1;
	private static final int GRADE_WEIGHT = 3;	// 10
	private static final int SCHOOL_WEIGHT = 10;	// 10
	private static final LevenshteinDistance LD = LevenshteinDistance.getDefaultInstance();

	@Override
	public int applyAsInt(Student lhsStudent, Student rhsStudent) {
		int lastNameDist = LD.apply(lhsStudent.lastName(), rhsStudent.lastName());
		int firstNameDist = Math.min(
			LD.apply(lhsStudent.firstName(), rhsStudent.firstName()),
			LD.apply(lhsStudent.nickName(), rhsStudent.firstName()));
		firstNameDist = Math.min(firstNameDist,
			LD.apply(rhsStudent.nickName(), lhsStudent.firstName()));
		int gradeDist = Math.abs(lhsStudent.grade() - rhsStudent.grade());
		int schoolDist = LD.apply(lhsStudent.school(), rhsStudent.school());
		return LAST_WEIGHT * lastNameDist
			+ FIRST_WEIGHT * firstNameDist
			+ GRADE_WEIGHT * gradeDist
			+ SCHOOL_WEIGHT * schoolDist;
	}
}
