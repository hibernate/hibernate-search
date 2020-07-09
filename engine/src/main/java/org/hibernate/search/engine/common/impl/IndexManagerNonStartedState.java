/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceExtractor;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.reporting.EventContext;

class IndexManagerNonStartedState {

	private final EventContext eventContext;
	private final ConfigurationPropertySourceExtractor propertySourceExtractor;
	private final IndexManagerImplementor indexManager;

	IndexManagerNonStartedState(EventContext eventContext,
			ConfigurationPropertySourceExtractor propertySourceExtractor,
			IndexManagerImplementor indexManager) {
		this.eventContext = eventContext;
		this.propertySourceExtractor = propertySourceExtractor;
		this.indexManager = indexManager;
	}

	void closeOnFailure() {
		indexManager.stop();
	}

	IndexManagerImplementor start(RootFailureCollector rootFailureCollector,
			BeanResolver beanResolver,
			ConfigurationPropertySource rootPropertySource) {
		ContextualFailureCollector indexFailureCollector = rootFailureCollector.withContext( eventContext );
		ConfigurationPropertySource indexPropertySource = propertySourceExtractor.extract( rootPropertySource );
		IndexManagerStartContextImpl startContext = new IndexManagerStartContextImpl(
				indexFailureCollector, beanResolver, indexPropertySource
		);
		try {
			indexManager.start( startContext );
		}
		catch (RuntimeException e) {
			indexFailureCollector.add( e );
		}
		return indexManager; // The index is now started
	}

}
