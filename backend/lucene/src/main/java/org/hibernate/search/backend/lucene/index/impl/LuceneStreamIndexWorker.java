/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import org.hibernate.search.engine.backend.index.spi.StreamIndexWorker;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneIndexWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;


/**
 * @author Guillaume Smet
 */
class LuceneStreamIndexWorker extends LuceneIndexWorker implements StreamIndexWorker<LuceneRootDocumentBuilder> {

	private final LuceneIndexWorkOrchestrator orchestrator;

	LuceneStreamIndexWorker(LuceneWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy,
			LuceneIndexWorkOrchestrator orchestrator,
			String indexName, SessionContext sessionContext) {
		super( factory, multiTenancyStrategy, indexName, sessionContext );
		this.orchestrator = orchestrator;
	}

	@Override
	protected void collect(LuceneIndexWork<?> work) {
		orchestrator.submit( work );
	}

	@Override
	public void flush() {
		collect( factory.flush( indexName ) );
	}

	@Override
	public void optimize() {
		collect( factory.optimize( indexName ) );
	}
}
