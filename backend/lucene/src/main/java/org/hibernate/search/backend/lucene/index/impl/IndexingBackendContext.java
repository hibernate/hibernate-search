/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;

import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.StreamIndexWorker;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.common.spi.SessionContext;

import org.apache.lucene.store.Directory;

public class IndexingBackendContext {
	// TODO use a dedicated object for the error context instead of the backend
	private final BackendImplementor<?> backend;

	private final DirectoryProvider directoryProvider;
	private final LuceneWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	public IndexingBackendContext(BackendImplementor<?> backend,
			DirectoryProvider directoryProvider,
			LuceneWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy) {
		this.backend = backend;
		this.directoryProvider = directoryProvider;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workFactory = workFactory;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[backend=" + backend + "]";
	}

	// TODO use a dedicated object for the error context instead of the backend
	BackendImplementor<?> getBackendImplementor() {
		return backend;
	}

	Directory createDirectory(String indexName) throws IOException {
		return directoryProvider.createDirectory( indexName );
	}

	ChangesetIndexWorker<LuceneRootDocumentBuilder> createChangesetIndexWorker(
			LuceneIndexWorkOrchestrator orchestrator,
			String indexName, SessionContext sessionContext) {
		multiTenancyStrategy.checkTenantId( backend, sessionContext.getTenantIdentifier() );

		return new LuceneChangesetIndexWorker( workFactory, multiTenancyStrategy, orchestrator,
				indexName, sessionContext );
	}

	StreamIndexWorker<LuceneRootDocumentBuilder> createStreamIndexWorker(
			LuceneIndexWorkOrchestrator orchestrator,
			String indexName, SessionContext sessionContext) {
		multiTenancyStrategy.checkTenantId( backend, sessionContext.getTenantIdentifier() );

		return new LuceneStreamIndexWorker( workFactory, multiTenancyStrategy, orchestrator,
				indexName, sessionContext );
	}
}
