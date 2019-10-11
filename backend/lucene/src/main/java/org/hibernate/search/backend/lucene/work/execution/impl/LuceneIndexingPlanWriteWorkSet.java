/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkSet;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

class LuceneIndexingPlanWriteWorkSet implements LuceneWriteWorkSet {
	private final List<LuceneWriteWork<?>> works;
	private final CompletableFuture<Object> future;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	LuceneIndexingPlanWriteWorkSet(List<LuceneWriteWork<?>> works, CompletableFuture<Object> future,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.works = new ArrayList<>( works );
		this.future = future;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void submitTo(LuceneWriteWorkProcessor processor) {
		processor.beforeWorkSet( commitStrategy, refreshStrategy );
		for ( LuceneWriteWork<?> work : works ) {
			processor.submit( work );
		}
		processor.afterWorkSet( future, null );
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}
}
