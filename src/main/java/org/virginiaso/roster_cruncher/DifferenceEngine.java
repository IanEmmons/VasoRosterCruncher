package org.virginiaso.roster_cruncher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DifferenceEngine {
	private static final int DISTANCE_THRESHOLD = 4;

	private final Set<Student> pStudents;
	private final Set<Student> sStudents;
	private final List<Match> matches;
	private final Set<Student> pStudentsNotFoundInS;
	private final Set<Student> sStudentsNotFoundInP;

	// Map of Scilympiad students to a map of distance to list of Portal students
	private final Map<Student, Map<Integer, List<Student>>> results;

	public static DifferenceEngine compare(List<Match> manualMatches,
			List<Student> pStudents, Set<Student> sStudents,
			DistanceFunction distanceFunction) {
		Stopwatch timer = new Stopwatch();
		DifferenceEngine engine = new DifferenceEngine(manualMatches, pStudents, sStudents);
		engine.compare(distanceFunction);
		timer.stopAndReport("Performed comparison");
		return engine;
	}

	private DifferenceEngine(List<Match> manualMatches, List<Student> pStudentList,
			Set<Student> sStudentList) {
		pStudents = new TreeSet<>(pStudentList);
		sStudents = new TreeSet<>(sStudentList);

		matches = new ArrayList<>();
		for (Match match : manualMatches) {
			// First, get the student from each list equal to the corresponding
			// student in the match.  This ensures that we are preserving the most
			// recent values of the student fields that are not used in the equality
			// comparison (like team number).
			Student pStudent = pStudents.stream()
				.filter(match.getPStudent()::equals)
				.findFirst()
				.orElse(null);
			Student sStudent = sStudents.stream()
				.filter(match.getSStudent()::equals)
				.findFirst()
				.orElse(null);
			if (pStudent == null || sStudent == null) {
				// Skip this match -- it's now invalid, because one of the two
				// students no longer appears in the corresponding input file.
			} else if (match.getVerdict() == Verdict.DIFFERENT) {
				matches.add(new Match(sStudent, pStudent, match.getVerdict()));
			} else if (match.getVerdict() == Verdict.SAME) {
				matches.add(new Match(sStudent, pStudent, match.getVerdict()));
				pStudents.remove(pStudent);
				sStudents.remove(sStudent);
			} else {
				throw new IllegalStateException(
					"Master report verdicts should be 'Different' or 'Same', not '%1$s'"
						.formatted(match.getVerdict()));
			}
		}

		pStudentsNotFoundInS = new TreeSet<>();
		sStudentsNotFoundInP = new TreeSet<>();
		results = new TreeMap<>();
	}

	private void compare(DistanceFunction distanceFunction) {
		for (Student pStudent : pStudents) {
			for (Student sStudent : sStudents) {
				boolean pairIsMarkedAsDifferent = matches.stream()
					.filter(match -> Verdict.DIFFERENT.equals(match.getVerdict()))
					.filter(match -> match.getPStudent().equals(pStudent))
					.filter(match -> match.getSStudent().equals(sStudent))
					.findAny()
					.isPresent();
				int distance = pairIsMarkedAsDifferent
					? Integer.MAX_VALUE
					: distanceFunction.applyAsInt(pStudent, sStudent);
				results
					.computeIfAbsent(sStudent, key -> new TreeMap<>())
					.computeIfAbsent(distance, key -> new ArrayList<>())
					.add(pStudent);
			}
		}

		// Compile a list of exact matches:
		for (Map.Entry<Student, Map<Integer, List<Student>>> entry : results.entrySet()) {
			Student sStudent = entry.getKey();
			List<Student> exactMatches = entry.getValue().get(0);
			if (exactMatches != null) {
				exactMatches.stream()
					.map(pStudent -> new Match(sStudent, pStudent, Verdict.EXACT_MATCH))
					.forEach(matches::add);
			}
		}

		// Remove the exact matches from the results data structure:
		for (Match match : matches) {
			if (match.getVerdict() == Verdict.EXACT_MATCH) {
				results.remove(match.getSStudent());
				for (Map<Integer, List<Student>> outerValue : results.values()) {
					for (List<Student> innerValue : outerValue.values()) {
						innerValue.remove(match.getPStudent());
					}
				}
			}
		}

		// Threshold the results data structure:
		for (Map<Integer, List<Student>> outerValue : results.values()) {
			List<Integer> keysToRemove = outerValue.keySet().stream()
				.filter(key -> key > DISTANCE_THRESHOLD)
				.collect(Collectors.toList());
			keysToRemove.forEach(key -> outerValue.remove(key));
		}

		// Remove empty entries in the results data structure:
		for (Map.Entry<Student, Map<Integer, List<Student>>> entry : results.entrySet()) {
			List<Integer> keysToRemove = entry.getValue().entrySet().stream()
				.filter(innerEntry -> innerEntry.getValue().isEmpty())
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
			keysToRemove.forEach(key -> entry.getValue().remove(key));
		}
		Iterator<Map.Entry<Student, Map<Integer, List<Student>>>> it
			= results.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Student, Map<Integer, List<Student>>> entry = it.next();
			if (entry.getValue().isEmpty()) {
				it.remove();
			}
		}

		// Compile sets of unmatched students:
		pStudentsNotFoundInS.addAll(pStudents);
		sStudentsNotFoundInP.addAll(sStudents);
		for (Match match : matches) {
			if (match.getVerdict() == Verdict.EXACT_MATCH) {
				pStudentsNotFoundInS.remove(match.getPStudent());
				sStudentsNotFoundInP.remove(match.getSStudent());
			}
		}
	}

	public List<Match> getMatches() {
		return matches;
	}

	public Set<Student> getPStudentsNotFoundInS() {
		return pStudentsNotFoundInS;
	}

	public Set<Student> getSStudentsNotFoundInP() {
		return sStudentsNotFoundInP;
	}

	/**
	 * Returns the results of the comparison.
	 *
	 * @return A map of s-student -> map of distance -> list of p-students at that
	 *         distance from the s-student
	 */
	public Map<Student, Map<Integer, List<Student>>> getResults() {
		return results;
	}

	/* Maps distances in the results data structure to counts of those distances */
	public Map<Integer, Integer> computeDistanceHistogram() {
		Map<Integer, Integer> distanceHistogram = new TreeMap<>();
		for (Map<Integer, List<Student>> value : results.values()) {
			for (Map.Entry<Integer, List<Student>> entry : value.entrySet()) {
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
			.map(entry -> "%1$2d: %2$2d".formatted(entry.getKey(), entry.getValue()))
			.collect(Collectors.joining(
				"%n   ".formatted(),
				"Histogram of distances:%n   ".formatted(),
				"%n".formatted()));
	}
}
