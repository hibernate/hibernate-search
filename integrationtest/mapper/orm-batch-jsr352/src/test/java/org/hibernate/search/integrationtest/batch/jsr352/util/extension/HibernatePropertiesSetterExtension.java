/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.util.extension;

import org.hibernate.cfg.Environment;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.SearchContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Need to mark all the test in this module with this extension as {@code EntityManagerFactoryRetrievalIT} is using
 * a non parametrized way to create hibernate resources. That means that {@link Environment#getProperties()} could cache
 * the state of settings that do not include all required resources and then there will be no way to override it.
 */
public class HibernatePropertiesSetterExtension implements BeforeAllCallback {
	@Override
	public void beforeAll(ExtensionContext context) {
		// if we run the ES version of the tests we have to set correct connection info based on testcontainer:
		if ( "elasticsearch_pu".equals( PersistenceUnitTestUtil.getPersistenceUnitName() ) ) {
			System.setProperty(
					BackendSettings.backendKey( ElasticsearchBackendSettings.URIS ),
					SearchContainer.connectionUrl()
			);
		}
		// make sure that DB is available and correctly configured:
		DatabaseContainer.configuration();
	}
}
