/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.SearchContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.springframework.test.context.ActiveProfilesResolver;

public class TestActiveProfilesResolver implements ActiveProfilesResolver {

	@Override
	public String[] resolve(Class<?> testClass) {
		String testBackend = System.getProperty( "test.backend" );
		if ( testBackend == null ) {
			/*
			 * Default when running tests from within an IDE.
			 * This is the main reason we're using an ActiveProfilesResolver:
			 * there is apparently no way to set default profiles for tests,
			 * as setting "spring.profiles.active" in a @TestPropertySource for example
			 * will *override* any command-line arguments, environment properties or system properties.
			 */
			testBackend = "lucene";
		}
		if ( "elasticsearch".equals( testBackend ) ) {
			System.setProperty( "ES_HOSTS", SearchContainer.host() + ":" + SearchContainer.mappedPort( 9200 ) );
		}
		DatabaseContainer.springConfiguration();
		// The test profiles must be mentioned last, to allow them to override properties
		return new String[] { testBackend, "test", "test-" + testBackend };
	}
}
