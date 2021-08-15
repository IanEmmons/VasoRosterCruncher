package org.virginiaso.roster_diff;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class AppTest {
	@Test
	public void appHasAGreeting() {
		assertNotNull(App.getGreeting(), "app should have a greeting");
	}
}
