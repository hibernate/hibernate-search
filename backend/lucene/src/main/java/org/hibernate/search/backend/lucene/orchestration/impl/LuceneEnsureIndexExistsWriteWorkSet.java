/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

/**
 * A special workset that won't trigger the creation of the index writer.
 * <p>
 * Useful to make sure that read-only applications never create any index writer.
 */
class LuceneEnsureIndexExistsWriteWorkSet implements LuceneWriteWorkSet {
	private final CompletableFuture<?> future;

	LuceneEnsureIndexExistsWriteWorkSet(CompletableFuture<?> future) {
		this.future = future;
	}

	@Override
	public void submitTo(LuceneWriteWorkProcessor processor) {
		processor.beforeWorkSet( DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
		processor.ensureIndexExists();
		processor.afterWorkSet( future, null );
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}
}
