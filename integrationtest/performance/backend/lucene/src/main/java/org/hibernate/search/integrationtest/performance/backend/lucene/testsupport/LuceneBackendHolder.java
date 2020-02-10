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
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.filesystem.TemporaryFileHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.AbstractBackendHolder;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LuceneBackendHolder extends AbstractBackendHolder {

	@Override
	protected ConfigurationPropertySource getDefaultBackendProperties(TemporaryFileHolder temporaryFileHolder)
			throws IOException {
		Map<String, Object> map = new LinkedHashMap<>();

		map.put( BackendSettings.TYPE, LuceneBackendSettings.TYPE_NAME );
		map.put( LuceneBackendSettings.DIRECTORY_ROOT, temporaryFileHolder.getIndexesDirectory().toAbsolutePath() );
		map.put( LuceneBackendSettings.ANALYSIS_CONFIGURER, LucenePerformanceAnalysisConfigurer.class );

		return ConfigurationPropertySource.fromMap( map );
	}
}
