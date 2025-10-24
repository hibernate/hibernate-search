/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.opensearch.rest.cfg.spi;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Configuration properties for the Elasticsearch backend that are considered SPI (and not API).
 */
@Incubating
public final class ClientOpenSearchElasticsearchBackendClientSpiSettings {

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property.
	 */
	public static final String PREFIX = EngineSettings.PREFIX + "backend.";

	/**
	 * An external Elasticsearch client instance that Hibernate Search should use for all requests to Elasticsearch.
	 * <p>
	 * If this is set, Hibernate Search will not attempt to create its own Elasticsearch,
	 * and all other client-related configuration properties
	 * (hosts/uris, authentication, discovery, timeouts, max connections, configurer, ...)
	 * will be ignored.
	 * <p>
	 * Expects a reference to a bean of type {@link org.opensearch.client.RestClient}.
	 * <p>
	 * Defaults to nothing: if no client instance is provided, Hibernate Search will create its own.
	 * <p>
	 * <strong>WARNING - Incubating API:</strong> the underlying client class may change without prior notice.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String CLIENT_INSTANCE = "client.instance";

	private ClientOpenSearchElasticsearchBackendClientSpiSettings() {
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static class Radicals {

		private Radicals() {
		}
	}

	public static final class Defaults {

		private Defaults() {
		}
	}
}
