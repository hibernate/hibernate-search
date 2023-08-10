/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public class StubIndexCreateContext {

	private final String indexName;
	private final ConfigurationPropertySource propertySource;

	public StubIndexCreateContext(String indexName, ConfigurationPropertySource propertySource) {
		this.indexName = indexName;
		this.propertySource = propertySource;
	}

	public String getIndexName() {
		return indexName;
	}

	public ConfigurationPropertySource getPropertySource() {
		return propertySource;
	}
}
