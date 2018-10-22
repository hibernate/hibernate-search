/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;

import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.EventContext;

import org.apache.lucene.store.Directory;

public class IndexingBackendContext {
	private final EventContext eventContext;

	private final DirectoryProvider directoryProvider;
	private final LuceneWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	public IndexingBackendContext(EventContext eventContext,
			DirectoryProvider directoryProvider,
			LuceneWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy) {
		this.eventContext = eventContext;
		this.directoryProvider = directoryProvider;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workFactory = workFactory;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	EventContext getEventContext() {
		return eventContext;
	}

	Directory createDirectory(String indexName) throws IOException {
		return directoryProvider.createDirectory( indexName );
	}

	IndexWorkPlan<LuceneRootDocumentBuilder> createWorkPlan(
			LuceneIndexWorkOrchestrator orchestrator,
			String indexName, SessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new LuceneIndexWorkPlan( workFactory, multiTenancyStrategy, orchestrator,
				indexName, sessionContext );
	}
}
