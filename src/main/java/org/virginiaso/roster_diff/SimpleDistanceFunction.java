package org.virginiaso.roster_diff;

public class SimpleDistanceFunction implements DistanceFunction {
	@Override
	public int applyAsInt(Student pStudent, ScilympiadStudent sStudent) {
		if (pStudent.lastName().equalsIgnoreCase(sStudent.lastName)
			&& (pStudent.firstName().equalsIgnoreCase(sStudent.firstName)
				|| pStudent.nickName().equalsIgnoreCase(sStudent.firstName))
			&& pStudent.grade() == sStudent.grade) {
			return 0;
		} else {
			return Integer.MAX_VALUE;
		}
	}
}
