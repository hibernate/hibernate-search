/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.util;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.SearchBackendContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

public final class HibernateSearchBatchTestConnectionProperties {
	private HibernateSearchBatchTestConnectionProperties() {
	}

	public static Map<String, Object> connectionProperties() {
		Map<String, Object> properties = new HashMap<>();
		// if we run the ES version of the tests we have to set correct connection info based on testcontainer:
		if ( "elasticsearch_pu".equals( PersistenceUnitTestUtil.getPersistenceUnitName() ) ) {
			properties.put(
					BackendSettings.backendKey( ElasticsearchBackendSettings.URIS ),
					SearchBackendContainer.connectionUrl()
			);
		}
		DatabaseContainer.configuration().add( properties );

		return properties;
	}
}
