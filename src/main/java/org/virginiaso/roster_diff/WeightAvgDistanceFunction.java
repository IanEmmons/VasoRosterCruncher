package org.virginiaso.roster_diff;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class WeightAvgDistanceFunction implements DistanceFunction {
	private static final int LAST_WEIGHT = 1;		// 2 or 1
	private static final int FIRST_WEIGHT = 1;
	private static final int GRADE_WEIGHT = 3;	// 10
	private static final int SCHOOL_WEIGHT = 10;	// 10
	private static final LevenshteinDistance LD = LevenshteinDistance.getDefaultInstance();

	@Override
	public int applyAsInt(PortalStudent pStudent, ScilympiadStudent sStudent) {
		int lastNameDist = LD.apply(pStudent.lastName, sStudent.lastName);
		int firstNameDist = Math.min(
			LD.apply(pStudent.firstName, sStudent.firstName),
			LD.apply(pStudent.nickName, sStudent.firstName));
		int gradeDist = Math.abs(pStudent.grade - sStudent.grade);
		int schoolDist = LD.apply(pStudent.school, sStudent.school);
		return LAST_WEIGHT * lastNameDist
			+ FIRST_WEIGHT * firstNameDist
			+ GRADE_WEIGHT * gradeDist
			+ SCHOOL_WEIGHT * schoolDist;
	}
}
