/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport;

import static org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration.BACKEND_TYPE;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public class BackendConfigurations {

	private BackendConfigurations() {
	}

	public static BackendConfiguration simple() {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return new LuceneBackendConfiguration();
			case "elasticsearch":
				return new ElasticsearchBackendConfiguration();
			default:
				throw new IllegalStateException( "Unknown backend type:" + BACKEND_TYPE );
		}
	}

}
