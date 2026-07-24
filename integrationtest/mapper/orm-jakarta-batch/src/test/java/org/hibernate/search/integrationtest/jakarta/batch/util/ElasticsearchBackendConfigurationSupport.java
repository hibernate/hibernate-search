/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.util;


import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public final class ElasticsearchBackendConfigurationSupport {

	private ElasticsearchBackendConfigurationSupport() {
	}

	public static BackendConfiguration simple() {
		return new ElasticsearchBackendConfiguration();
	}

}
