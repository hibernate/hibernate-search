/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.EngineConfigurationUtils;
import org.hibernate.search.engine.reporting.impl.RootFailureCollector;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;

class IndexManagerPartialBuildState {

	private final String backendName;
	private final String indexName;
	private final IndexManagerImplementor<?> partiallyBuiltIndexManager;

	IndexManagerPartialBuildState(String backendName,
			String indexName, IndexManagerImplementor<?> partiallyBuiltIndexManager) {
		this.backendName = backendName;
		this.indexName = indexName;
		this.partiallyBuiltIndexManager = partiallyBuiltIndexManager;
	}

	void closeOnFailure() {
		partiallyBuiltIndexManager.close();
	}

	IndexManagerImplementor<?> finalizeBuild(RootFailureCollector rootFailureCollector,
			ConfigurationPropertySource rootPropertySource) {
		ContextualFailureCollector indexFailureCollector =
				rootFailureCollector.withContext( EventContexts.fromIndexName( indexName ) );
		ConfigurationPropertySource backendPropertySource =
				EngineConfigurationUtils.getBackend( rootPropertySource, backendName );
		ConfigurationPropertySource indexPropertySource =
				EngineConfigurationUtils.getIndex(
						backendPropertySource,
						EngineConfigurationUtils.getIndexDefaults( backendPropertySource ),
						indexName
				);
		IndexManagerStartContextImpl startContext = new IndexManagerStartContextImpl(
				indexFailureCollector, indexPropertySource
		);
		try {
			partiallyBuiltIndexManager.start( startContext );
		}
		catch (RuntimeException e) {
			indexFailureCollector.add( e );
		}
		return partiallyBuiltIndexManager; // The index manager is now fully built
	}
}
