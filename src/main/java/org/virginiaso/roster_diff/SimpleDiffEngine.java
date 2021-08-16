package org.virginiaso.roster_diff;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class SimpleDiffEngine {
	private final List<PortalStudent> pStudents;
	private final List<ScilympiadStudent> sStudents;
	private final List<Pair<PortalStudent, ScilympiadStudent>> matches;
	private final List<PortalStudent> pStudentsNotFoundInS;
	private final List<ScilympiadStudent> sStudentsNotFoundInP;

	public static SimpleDiffEngine compare(List<PortalStudent> pStudents,
			List<ScilympiadStudent> sStudents) {
		Stopwatch timer = new Stopwatch();
		SimpleDiffEngine engine = new SimpleDiffEngine(pStudents, sStudents);
		engine.compare();
		timer.stopAndReport("Performed simple comparison");
		return engine;
	}

	private SimpleDiffEngine(List<PortalStudent> pStudents,
			List<ScilympiadStudent> sStudents) {
		this.pStudents = pStudents;
		this.sStudents = sStudents;
		matches = new ArrayList<>();
		pStudentsNotFoundInS = new ArrayList<>();
		sStudentsNotFoundInP = new ArrayList<>();
	}

	private void compare() {
		for (PortalStudent pStudent : pStudents) {
			for (ScilympiadStudent sStudent : sStudents) {
				if (pStudent.lastName.equalsIgnoreCase(sStudent.lastName)
					&& (pStudent.firstName.equalsIgnoreCase(sStudent.firstName)
						|| pStudent.nickName.equalsIgnoreCase(sStudent.firstName))) {
					matches.add(Pair.of(pStudent, sStudent));
				}
			}
		}
	}

	public List<Pair<PortalStudent, ScilympiadStudent>> getMatches() {
		return matches;
	}

	public List<PortalStudent> getpStudentsNotFoundInS() {
		return pStudentsNotFoundInS;
	}

	public List<ScilympiadStudent> getsStudentsNotFoundInP() {
		return sStudentsNotFoundInP;
	}
}
