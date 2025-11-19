/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.jdk.cfg;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.jdk.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Specific configuration properties for the Elasticsearch backend's rest client based on the Elasticsearch's low-level rest client.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 *
 * @author Gunnar Morling
 */
@Incubating
public final class ClientJdkElasticsearchBackendClientSettings {

	private ClientJdkElasticsearchBackendClientSettings() {
	}

	/**
	 * The timeout when executing a request to an Elasticsearch server.
	 * <p>
	 * This includes the time needed to establish a connection, send the request and read the response.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as 60000,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to no request timeout.
	 */
	public static final String REQUEST_TIMEOUT = "request_timeout";

	/**
	 * The timeout when establishing a connection to an Elasticsearch server.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 3000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#CONNECTION_TIMEOUT}.
	 */
	public static final String CONNECTION_TIMEOUT = "connection_timeout";

	/**
	* A {@link ElasticsearchHttpClientConfigurer} that defines custom HTTP client configuration.
	* <p>
	* It can be used for example to tune the SSL context to accept self-signed certificates.
	* It allows overriding other HTTP client settings, such as {@link ElasticsearchBackendSettings#USERNAME}.
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

		public static final int CONNECTION_TIMEOUT = 1000;
	}
}
