package org.virginiaso.roster_cruncher;

public class Stopwatch {
	private long start;
	private double duration;

	public Stopwatch() {
		start = System.currentTimeMillis();
		duration = -1.0;
	}

	public void stop() {
		duration = (System.currentTimeMillis() - start) / 1000.0;
	}

	public void report(String messageFormat, Object... args) {
		String message = messageFormat.formatted(args);
		System.out.format("%1$s in %2$.1f seconds%n", message, duration);
	}

	public void stopAndReport(String messageFormat, Object... args) {
		stop();
		report(messageFormat, args);
	}
}
