/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.EngineConfigurationUtils;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

class IndexManagerNonStartedState {

	private final String backendName;
	private final String indexName;
	private final IndexManagerImplementor indexManager;

	IndexManagerNonStartedState(String backendName,
			String indexName, IndexManagerImplementor indexManager) {
		this.backendName = backendName;
		this.indexName = indexName;
		this.indexManager = indexManager;
	}

	void closeOnFailure() {
		indexManager.stop();
	}

	CompletableFuture<?> start(RootFailureCollector rootFailureCollector,
			BeanResolver beanResolver,
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
				indexFailureCollector, beanResolver, indexPropertySource
		);
		return indexManager.start( startContext )
				.exceptionally( Futures.handler( e -> {
					indexFailureCollector.add( Throwables.expectException( e ) );
					return null;
				} ) );
	}

	public IndexManagerImplementor getIndexManager() {
		return indexManager;
	}
}
