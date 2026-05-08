/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport;


import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public final class ElasticsearchBackendConfigurationSupport {

	private ElasticsearchBackendConfigurationSupport() {
	}

	public static BackendConfiguration simple() {
		return new ElasticsearchBackendConfiguration();
	}

	public static boolean isVectorSearchSupportedByElasticsearch() {
		return ElasticsearchTestDialect.isActualVersion(
				es -> !es.isLessThan( "8.12.0" ),
				os -> !os.isLessThan( "2.9.0" ),
				aoss -> true
		);
	}
}
