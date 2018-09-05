/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.test.util;

/**
 * @author Yoann Rodiere
 */
public final class PersistenceUnitTestUtil {

	private static final String PERSISTENCE_UNIT_NAME_PROPERTY = "org.hibernate.search.jsr352.persistence_unit";

	// Mainly for convenience, to run tests in the IDE
	private static final String DEFAULT_PERSISTENCE_UNIT_NAME = "lucene_pu";

	private PersistenceUnitTestUtil() {
		// Private constructor
	}

	/**
	 * @return The persistence unit to use for tests. Allows us to run tests multiple times,
	 * using different settings.
	 */
	public static String getPersistenceUnitName() {
		String result = System.getProperty( PERSISTENCE_UNIT_NAME_PROPERTY );
		return result == null ? DEFAULT_PERSISTENCE_UNIT_NAME : result;
	}
}
