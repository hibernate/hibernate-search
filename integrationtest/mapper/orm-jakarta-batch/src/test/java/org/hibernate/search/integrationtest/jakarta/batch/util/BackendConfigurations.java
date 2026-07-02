/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.util;

import static org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration.BACKEND_TYPE;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public final class BackendConfigurations {

	private BackendConfigurations() {
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

}
