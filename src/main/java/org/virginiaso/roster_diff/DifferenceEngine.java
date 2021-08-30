package org.virginiaso.roster_diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class DifferenceEngine {
	private static final int DISTANCE_THRESHOLD = 6;

	private final List<PortalStudent> pStudents;
	private final List<ScilympiadStudent> sStudents;
	private final List<Pair<ScilympiadStudent, PortalStudent>> exactMatches;
	private final Set<PortalStudent> pStudentsNotFoundInS;
	private final Set<ScilympiadStudent> sStudentsNotFoundInP;
	private final Map<ScilympiadStudent, Map<Integer, List<PortalStudent>>> results;

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
		exactMatches = new ArrayList<>();
		pStudentsNotFoundInS = new TreeSet<>();
		sStudentsNotFoundInP = new TreeSet<>();
		results = new TreeMap<>();
	}

	private void compare(DistanceFunction distanceFunction) {
		for (PortalStudent pStudent : pStudents) {
			for (ScilympiadStudent sStudent : sStudents) {
				int distance = distanceFunction.applyAsInt(pStudent, sStudent);
				results
					.computeIfAbsent(sStudent, key -> new TreeMap<>())
					.computeIfAbsent(distance, key -> new ArrayList<>())
					.add(pStudent);
			}
		}

		// Compile a list of exact matches:
		for (Map.Entry<ScilympiadStudent, Map<Integer, List<PortalStudent>>> entry : results.entrySet()) {
			ScilympiadStudent sStudent = entry.getKey();
			Map<Integer, List<PortalStudent>> matches = entry.getValue();
			List<PortalStudent> zeros = matches.get(0);
			if (zeros != null) {
				zeros.stream()
					.map(pStudent -> Pair.of(sStudent, pStudent))
					.forEach(exactMatches::add);
			}
		}

		// Remove the exact matches from the results data structure:
		for (Pair<ScilympiadStudent, PortalStudent> pair : exactMatches) {
			ScilympiadStudent sStudent = pair.getLeft();
			PortalStudent pStudent = pair.getRight();
			results.remove(sStudent);
			for (Map<Integer, List<PortalStudent>> outerValue : results.values()) {
				for (List<PortalStudent> innerValue : outerValue.values()) {
					innerValue.remove(pStudent);
				}
			}
		}

		// Threshold the results data structure:
		for (Map<Integer, List<PortalStudent>> outerValue : results.values()) {
			List<Integer> keysToRemove = outerValue.keySet().stream()
				.filter(key -> key > DISTANCE_THRESHOLD)
				.collect(Collectors.toList());
			keysToRemove.forEach(key -> outerValue.remove(key));
		}

		// Remove empty entries in the results data structure:
		for (Map.Entry<ScilympiadStudent, Map<Integer, List<PortalStudent>>> entry : results.entrySet()) {
			List<Integer> keysToRemove = entry.getValue().entrySet().stream()
				.filter(innerEntry -> innerEntry.getValue().isEmpty())
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
			keysToRemove.forEach(key -> entry.getValue().remove(key));
		}
		Iterator<Map.Entry<ScilympiadStudent, Map<Integer, List<PortalStudent>>>> it
			= results.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ScilympiadStudent, Map<Integer, List<PortalStudent>>> entry = it.next();
			if (entry.getValue().isEmpty()) {
				it.remove();
			}
		}

		// Compile sets of unmatched students:
		pStudentsNotFoundInS.addAll(pStudents);
		sStudentsNotFoundInP.addAll(sStudents);
		for (Pair<ScilympiadStudent, PortalStudent> pair : exactMatches) {
			ScilympiadStudent sStudent = pair.getLeft();
			PortalStudent pStudent = pair.getRight();
			pStudentsNotFoundInS.remove(pStudent);
			sStudentsNotFoundInP.remove(sStudent);
		}
		for (Map.Entry<ScilympiadStudent, Map<Integer, List<PortalStudent>>> entry : results.entrySet()) {
			ScilympiadStudent sStudent = entry.getKey();
			sStudentsNotFoundInP.remove(sStudent);
			entry.getValue().values().stream()
				.flatMap(List::stream)
				.forEach(pStudentsNotFoundInS::remove);
		}
	}

	public List<Pair<ScilympiadStudent, PortalStudent>> getExactMatches() {
		return exactMatches;
	}

	public Set<PortalStudent> getPStudentsNotFoundInS() {
		return pStudentsNotFoundInS;
	}

	public Set<ScilympiadStudent> getSStudentsNotFoundInP() {
		return sStudentsNotFoundInP;
	}

	/**
	 * Returns the results of the comparison.
	 *
	 * @return A map of s-student -> map of distance -> list of p-students at that
	 *         distance from the s-student
	 */
	public Map<ScilympiadStudent, Map<Integer, List<PortalStudent>>> getResults() {
		return results;
	}

	/* Maps distances in the results data structure to counts of those distances */
	public Map<Integer, Integer> computeDistanceHistogram() {
		Map<Integer, Integer> distanceHistogram = new TreeMap<>();
		for (Map<Integer, List<PortalStudent>> value : results.values()) {
			for (Map.Entry<Integer, List<PortalStudent>> entry : value.entrySet()) {
				Integer distance = entry.getKey();
				int numPStudents = entry.getValue().size();
				distanceHistogram.merge(distance, numPStudents,
					(existingCount, increment) -> existingCount + increment);
			}
		}
		return distanceHistogram;
	}

	public String formatDistanceHistogram() {
		return computeDistanceHistogram().entrySet().stream()
			.map(entry -> String.format("%1$2d: %2$4d", entry.getKey(), entry.getValue()))
			.collect(Collectors.joining(
				String.format("%n   "),
				String.format("Histogram of distances:%n   "),
				String.format("%n")));
	}
}
