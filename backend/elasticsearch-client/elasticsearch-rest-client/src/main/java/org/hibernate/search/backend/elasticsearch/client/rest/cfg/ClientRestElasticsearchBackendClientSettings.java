/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.rest.cfg;

import org.hibernate.search.backend.elasticsearch.client.common.cfg.ElasticsearchBackendClientCommonSettings;
import org.hibernate.search.backend.elasticsearch.client.rest.ElasticsearchHttpClientConfigurer;

/**
 * Specific configuration properties for the Elasticsearch backend's rest client based on the Elasticsearch's low-level rest client.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 *
 * @author Gunnar Morling
 */
public final class ClientRestElasticsearchBackendClientSettings {

	private ClientRestElasticsearchBackendClientSettings() {
	}

	/**
	* A {@link ElasticsearchHttpClientConfigurer} that defines custom HTTP client configuration.
	* <p>
	* It can be used for example to tune the SSL context to accept self-signed certificates.
	* It allows overriding other HTTP client settings, such as {@link ElasticsearchBackendClientCommonSettings#USERNAME} or {@link ElasticsearchBackendClientCommonSettings#MAX_CONNECTIONS_PER_ROUTE}.
	* <p>
	* Expects a reference to a bean of type {@link ElasticsearchHttpClientConfigurer}.
	* <p>
	* Defaults to no value.
	*/
	public static final String CLIENT_CONFIGURER = "client.configurer";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}
	}
}
