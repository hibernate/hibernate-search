/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
