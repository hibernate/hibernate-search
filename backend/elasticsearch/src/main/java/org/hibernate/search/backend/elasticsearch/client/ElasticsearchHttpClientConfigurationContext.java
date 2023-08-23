/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * The context passed to {@link ElasticsearchHttpClientConfigurer}.
 */
public interface ElasticsearchHttpClientConfigurationContext {

	/**
	 * @return A {@link BeanResolver}.
	 */
	BeanResolver beanResolver();

	/**
	 * @return A configuration property source, appropriately masked so that the factory
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property key "type" is reserved for use by the engine.
	 */
	ConfigurationPropertySource configurationPropertySource();

	/**
	 * @return An Apache HTTP client builder, to set the configuration.
	 * @see <a href="http://hc.apache.org/httpcomponents-client-ga/">the Apache HTTP Client documentation</a>
	 */
	HttpAsyncClientBuilder clientBuilder();

	/**
	 * @return The version of Elasticsearch/OpenSearch configured on the backend.
	 * May be empty if not configured explicitly (in which case it will only be known after the client is built).
	 */
	Optional<ElasticsearchVersion> configuredVersion();

}
