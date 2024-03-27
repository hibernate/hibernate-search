/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.elasticsearch.testsupport;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.filesystem.TemporaryFileHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.AbstractBackendHolder;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ElasticsearchBackendHolder extends AbstractBackendHolder {

	/**
	 * A list of configuration properties to apply to the backend and indexes.
	 * <p>
	 * Format: {@code <key>=<value>&<key2>=<value2>} (etc.).
	 * Multiple configurations can be tested by providing multiple values for this parameter,
	 * e.g. {@code foo=1&bar=2,foo=2&bar=1} for two configurations setting {@code foo} and {@code bar} to different values.
	 * <p>
	 * Note that configuration properties are applied both at the backend level and at the index level.
	 */
	@Param({ "", "max_connections_per_route=1" })
	private String configuration;

	@Override
	protected ConfigurationPropertySource getDefaultBackendProperties(TemporaryFileHolder temporaryFileHolder) {
		Map<String, Object> map = new LinkedHashMap<>();

		// Custom connection info is provided by setting system properties,
		// e.g. "uris" or "aws.signing.enabled".
		map.put( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER, ElasticsearchPerformanceAnalysisConfigurer.class );

		map.put( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS, IndexStatus.YELLOW );

		return AllAwareConfigurationPropertySource.fromMap( map );
	}

	@Override
	protected String getConfigurationParameter() {
		return configuration;
	}
}
