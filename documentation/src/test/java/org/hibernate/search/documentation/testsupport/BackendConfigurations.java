/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport;

import static org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration.BACKEND_TYPE;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public final class BackendConfigurations {

	private BackendConfigurations() {
	}

	// Plain configuration, without analysis configurers
	public static BackendConfiguration plain() {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return LuceneBackendConfigurationSupport.plain();
			case "elasticsearch":
				return ElasticsearchBackendConfigurationSupport.plain();
			default:
				throw new IllegalStateException( "Unknown backend type: " + BACKEND_TYPE );
		}
	}

	public static BackendConfiguration simple() {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return LuceneBackendConfigurationSupport.simple();
			case "elasticsearch":
				return ElasticsearchBackendConfigurationSupport.simple();
			default:
				throw new IllegalStateException( "Unknown backend type: " + BACKEND_TYPE );
		}
	}

	public static BackendConfiguration hashBasedSharding(int shardCount) {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return LuceneBackendConfigurationSupport.hashBasedSharding( shardCount );
			case "elasticsearch":
				return ElasticsearchBackendConfigurationSupport.hashBasedSharding( shardCount );
			default:
				throw new IllegalStateException( "Unknown backend type: " + BACKEND_TYPE );
		}
	}

}
