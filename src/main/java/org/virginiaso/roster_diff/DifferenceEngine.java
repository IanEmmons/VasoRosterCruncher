package org.virginiaso.roster_diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class DifferenceEngine {
	private final List<PortalStudent> pStudents;
	private final List<ScilympiadStudent> sStudents;
	private final List<Pair<PortalStudent, ScilympiadStudent>> matches;
	private final List<PortalStudent> pStudentsNotFoundInS;
	private final List<ScilympiadStudent> sStudentsNotFoundInP;
	private final Map<ScilympiadStudent, List<Pair<PortalStudent, Integer>>> results;

	public static DifferenceEngine compare(List<PortalStudent> pStudents,
			List<ScilympiadStudent> sStudents, DistanceFunction distanceFunction) {
		Stopwatch timer = new Stopwatch();
		DifferenceEngine engine = new DifferenceEngine(pStudents, sStudents);
		engine.compare(distanceFunction);
		timer.stopAndReport("Performed comparison");
		return engine;
	}

	private DifferenceEngine(List<PortalStudent> pStudents,
			List<ScilympiadStudent> sStudents) {
		this.pStudents = pStudents;
		this.sStudents = sStudents;
		matches = new ArrayList<>();
		pStudentsNotFoundInS = new ArrayList<>();
		sStudentsNotFoundInP = new ArrayList<>();
		results = new HashMap<>();
	}

	private void compare(DistanceFunction distanceFunction) {
		for (PortalStudent pStudent : pStudents) {
			for (ScilympiadStudent sStudent : sStudents) {
				int distance = distanceFunction.applyAsInt(pStudent, sStudent);
				results.computeIfAbsent(sStudent, key -> new ArrayList<>())
					.add(Pair.of(pStudent, distance));
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

	public Map<ScilympiadStudent, List<Pair<PortalStudent, Integer>>> getResults() {
		return results;
	}
}
