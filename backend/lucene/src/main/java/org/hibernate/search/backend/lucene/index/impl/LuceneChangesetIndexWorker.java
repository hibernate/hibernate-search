/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneIndexWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;


/**
 * @author Guillaume Smet
 */
public class LuceneChangesetIndexWorker extends LuceneIndexWorker implements ChangesetIndexWorker<LuceneRootDocumentBuilder> {

	private final LuceneIndexWorkOrchestrator orchestrator;
	private final List<LuceneIndexWork<?>> works = new ArrayList<>();

	public LuceneChangesetIndexWorker(LuceneWorkFactory factory,
			LuceneIndexWorkOrchestrator orchestrator,
			String indexName, SessionContext context) {
		super( factory, indexName, context );
		this.orchestrator = orchestrator;
	}

	@Override
	protected void collect(LuceneIndexWork<?> work) {
		works.add( work );
	}

	@Override
	public void prepare() {
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			CompletableFuture<?> future = orchestrator.submit( works );
			return future;
		}
		finally {
			works.clear();
		}
	}
}
