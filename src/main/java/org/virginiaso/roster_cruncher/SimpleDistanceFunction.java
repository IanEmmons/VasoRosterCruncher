package org.virginiaso.roster_cruncher;

public class SimpleDistanceFunction implements DistanceFunction {
	@Override
	public int applyAsInt(Student lhsStudent, Student rhsStudent) {
		if (lhsStudent.lastName().equalsIgnoreCase(rhsStudent.lastName())
			&& (lhsStudent.firstName().equalsIgnoreCase(rhsStudent.firstName())
				|| lhsStudent.nickName().equalsIgnoreCase(rhsStudent.firstName())
				|| rhsStudent.nickName().equalsIgnoreCase(lhsStudent.firstName()))
			&& lhsStudent.grade() == rhsStudent.grade()) {
			return 0;
		} else {
			return Integer.MAX_VALUE;
		}
	}
}
