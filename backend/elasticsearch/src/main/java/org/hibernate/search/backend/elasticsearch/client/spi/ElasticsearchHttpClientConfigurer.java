/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * An extension point allowing fine tuning of the Apache HTTP Client used by the Elasticsearch integration.
 * <p>
 * This enables in particular connecting to cloud services that require a particular authentication method,
 * such as request signing on Amazon Web Services.
 * <p>
 * The ElasticsearchHttpClientConfigurer implementation will be given access to the HTTP client builder
 * on startup.
 * <p>
 * Note that you don't have to configure the client unless you have specific needs:
 * the default configuration should work just fine for an on-premise Elasticsearch server.
 */
public interface ElasticsearchHttpClientConfigurer {

	/**
	 * Configure the HTTP Client.
	 * <p>
	 * This method is called once for every configurer, each time an Elasticsearch client is set up.
	 * <p>
	 * Implementors should take care of only applying configuration if relevant:
	 * there may be multiple, conflicting configurers in the path, so implementors should first check
	 * (through a configuration property) whether they are needed or not before applying any modification.
	 * For example an authentication configurer could decide not to do anything if no username is provided,
	 * or if the configuration property {@code my.configurer.enabled} is {@code false}.
	 *
	 * @param builder An Apache HTTP client builder, to set the configuration to be applied.
	 * @param propertySource The property source for the Elasticsearch backend being configured.
	 * Properties are masked, i.e. {@code hibernate.search.backend.my.property}
	 * will be accessed as simply {@code my.property}.
	 *
	 * @see <a href="http://hc.apache.org/httpcomponents-client-ga/">the Apache HTTP Client documentation</a>
	 */
	void configure(HttpAsyncClientBuilder builder, ConfigurationPropertySource propertySource);

}
