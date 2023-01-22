package org.virginiaso.roster_cruncher;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class WeightAvgDistanceFunction implements DistanceFunction {
	private static final int LAST_WEIGHT = 1;
	private static final int FIRST_WEIGHT = 1;
	private static final int GRADE_WEIGHT = 3;
	private static final int SCHOOL_WEIGHT = 10;
	private static final LevenshteinDistance LD = LevenshteinDistance.getDefaultInstance();

	@Override
	public int applyAsInt(Student lhsStudent, Student rhsStudent) {
		int forwardNameDist = forwardNameDistance(lhsStudent, rhsStudent);
		int backwardNameDist = backwardNameDistance(lhsStudent, rhsStudent);
		int nameDist = Math.min(forwardNameDist, backwardNameDist);
		int gradeDist = Math.abs(lhsStudent.grade() - rhsStudent.grade());
		int schoolDist = lowerLD(lhsStudent.school(), rhsStudent.school());
		return nameDist
			+ GRADE_WEIGHT * gradeDist
			+ SCHOOL_WEIGHT * schoolDist;
	}

	private static int forwardNameDistance(Student lhsStudent, Student rhsStudent) {
		int lastNameDist = lowerLD(lhsStudent.lastName(), rhsStudent.lastName());
		int firstNameDist = lowerLD(lhsStudent.firstName(), rhsStudent.firstName());
		firstNameDist = Math.min(firstNameDist,
			lowerLD(lhsStudent.nickName(), rhsStudent.firstName()));
		firstNameDist = Math.min(firstNameDist,
			lowerLD(rhsStudent.nickName(), lhsStudent.firstName()));
		return LAST_WEIGHT * lastNameDist
			+ FIRST_WEIGHT * firstNameDist;
	}

	private static int backwardNameDistance(Student lhsStudent, Student rhsStudent) {
		int lastNameDist = lowerLD(lhsStudent.lastName(), rhsStudent.firstName());
		int firstNameDist = lowerLD(lhsStudent.firstName(), rhsStudent.lastName());
		firstNameDist = Math.min(firstNameDist,
			lowerLD(lhsStudent.nickName(), rhsStudent.lastName()));
		firstNameDist = Math.min(firstNameDist,
			lowerLD(rhsStudent.nickName(), lhsStudent.lastName()));
		return LAST_WEIGHT * lastNameDist
			+ FIRST_WEIGHT * firstNameDist;
	}

	private static int lowerLD(String lhs, String rhs) {
		return LD.apply(lhs.toLowerCase(), rhs.toLowerCase());
	}
}
