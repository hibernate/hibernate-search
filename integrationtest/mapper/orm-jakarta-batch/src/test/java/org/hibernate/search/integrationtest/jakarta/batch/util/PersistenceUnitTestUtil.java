/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.util;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

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
		return ( BackendConfiguration.isElasticsearch() )
				? ELASTICSEARCH_PERSISTENCE_UNIT_NAME
				: LUCENE_PERSISTENCE_UNIT_NAME;
	}
}
