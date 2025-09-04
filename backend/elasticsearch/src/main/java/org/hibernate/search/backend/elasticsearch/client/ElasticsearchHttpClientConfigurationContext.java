/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;

/**
 * The context passed to {@link ElasticsearchHttpClientConfigurer}
 *
 * @deprecated Use the client specific configurers instead.
 */
@Deprecated(since = "8.2", forRemoval = true)
public interface ElasticsearchHttpClientConfigurationContext
		extends
		org.hibernate.search.backend.elasticsearch.client.rest.ElasticsearchHttpClientConfigurationContext {

	/**
	 * @return The version of Elasticsearch/OpenSearch configured on the backend.
	 * May be empty if not configured explicitly (in which case it will only be known after the client is built).
	 *
	 * @deprecated Use the {@link #configurationPropertySource() property source} and inspect the corresponding properties
	 * (e.g. {@link ElasticsearchBackendSettings#VERSION}) instead.
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	default Optional<ElasticsearchVersion> configuredVersion() {
		return ConfigurationProperty.forKey( ElasticsearchBackendSettings.VERSION )
				.as( ElasticsearchVersion.class, ElasticsearchVersion::of )
				.build().get( configurationPropertySource() );
	}

}
