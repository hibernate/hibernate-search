/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
