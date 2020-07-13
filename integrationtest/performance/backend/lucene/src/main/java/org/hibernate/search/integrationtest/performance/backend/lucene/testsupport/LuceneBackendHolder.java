/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.lucene.testsupport;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.filesystem.TemporaryFileHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.AbstractBackendHolder;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LuceneBackendHolder extends AbstractBackendHolder {

	/**
	 * A list of configuration properties to apply to the backend and indexes.
	 * <p>
	 * Format: {@code <key>=<value>&<key2>=<value2>} (etc.).
	 * Multiple configurations can be tested by providing multiple values for this parameter,
	 * e.g. {@code foo=1&bar=2,foo=2&bar=1} for two configurations setting {@code foo} and {@code bar} to different values.
	 * <p>
	 * Note that configuration properties are applied both at the backend level and at the index level,
	 * so using the "index_defaults." prefix is optional when setting index-level properties.
	 */
	@Param({ "", "io.refresh_interval=1000" })
	private String configuration;

	@Override
	protected ConfigurationPropertySource getDefaultBackendProperties(TemporaryFileHolder temporaryFileHolder)
			throws IOException {
		Map<String, Object> map = new LinkedHashMap<>();

		map.put( LuceneBackendSettings.DIRECTORY_ROOT, temporaryFileHolder.getIndexesDirectory().toAbsolutePath() );
		map.put( LuceneBackendSettings.ANALYSIS_CONFIGURER, LucenePerformanceAnalysisConfigurer.class );

		return ConfigurationPropertySource.fromMap( map );
	}

	@Override
	protected String getConfigurationParameter() {
		return configuration;
	}
}
