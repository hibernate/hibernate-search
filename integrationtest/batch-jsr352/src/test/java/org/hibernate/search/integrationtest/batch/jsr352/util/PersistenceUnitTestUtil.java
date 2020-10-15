/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.util;

import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

/**
 * @author Yoann Rodiere
 */
public final class PersistenceUnitTestUtil {

	private static final String LUCENE_PERSISTENCE_UNIT_NAME = "lucene_pu";
	private static final String ELASTICSEARCH_PERSISTENCE_UNIT_NAME = "elasticsearch_pu";

	private PersistenceUnitTestUtil() {
		// Private constructor
	}

	/**
	 * @return The persistence unit to use for tests. Allows us to run tests multiple times,
	 * using different settings.
	 */
	public static String getPersistenceUnitName() {
		return ( BackendConfiguration.isElasticsearch() ) ? ELASTICSEARCH_PERSISTENCE_UNIT_NAME
				: LUCENE_PERSISTENCE_UNIT_NAME;
	}
}
