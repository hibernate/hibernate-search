/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.IndexManagementWork;

/**
 * A special write workset that won't trigger the creation of the index writer.
 * <p>
 * Useful to make sure that read-only applications never create any index writer.
 */
class LuceneManagementWorkSet<T> implements LuceneWriteWorkSet {

	private final IndexManagementWork<T> work;

	private final CompletableFuture<T> future;

	LuceneManagementWorkSet(IndexManagementWork<T> work, CompletableFuture<T> future) {
		this.work = work;
		this.future = future;
	}

	@Override
	public void submitTo(LuceneWriteWorkProcessor processor) {
		try {
			T result = processor.submit( work );
			future.complete( result );
		}
		catch (RuntimeException e) {
			markAsFailed( e );
		}
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}
}
