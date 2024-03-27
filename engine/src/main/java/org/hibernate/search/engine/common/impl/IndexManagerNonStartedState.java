/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceExtractor;
import org.hibernate.search.engine.common.resources.spi.SavedState;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.reporting.EventContext;

class IndexManagerNonStartedState {

	private final EventContext eventContext;
	private final ConfigurationPropertySourceExtractor propertySourceExtractor;
	private final IndexManagerImplementor indexManager;

	// created on pre-start
	private IndexManagerStartContextImpl startContext;
	private ContextualFailureCollector indexFailureCollector;

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

	void preStart(RootFailureCollector rootFailureCollector, BeanResolver beanResolver,
			ConfigurationPropertySource rootPropertySource, SavedState savedState) {
		indexFailureCollector = rootFailureCollector.withContext( eventContext );
		ConfigurationPropertySource indexPropertySource = propertySourceExtractor.extract( beanResolver, rootPropertySource );
		startContext = new IndexManagerStartContextImpl(
				indexFailureCollector, beanResolver, indexPropertySource
		);
		try {
			indexManager.preStart( startContext, savedState );
		}
		catch (RuntimeException e) {
			indexFailureCollector.add( e );
		}
	}

	IndexManagerImplementor start() {
		try {
			indexManager.start( startContext );
		}
		catch (RuntimeException e) {
			indexFailureCollector.add( e );
		}
		return indexManager; // The index is now started
	}
}
