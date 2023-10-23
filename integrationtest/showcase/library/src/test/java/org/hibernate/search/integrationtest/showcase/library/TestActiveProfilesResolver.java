/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import org.springframework.test.context.ActiveProfilesResolver;

public class TestActiveProfilesResolver implements ActiveProfilesResolver {

	/*
	 * Default when running tests from within an IDE.
	 * This is the main reason we're using an ActiveProfilesResolver:
	 * there is apparently no way to set default profiles for tests,
	 * as setting "spring.profiles.active" in a @TestPropertySource for example
	 * will *override* any command-line arguments, environment properties or system properties.
	 */
	private static final String DEFAULT_BACKEND = "lucene";

	@Override
	public String[] resolve(Class<?> testClass) {
		String testBackend = configuredBackend();
		// The test profiles must be mentioned last, to allow them to override properties
		return new String[] { testBackend, "test", "test-" + testBackend };
	}

	public static String configuredBackend() {
		return System.getProperty( "test.backend", DEFAULT_BACKEND );
	}
}
