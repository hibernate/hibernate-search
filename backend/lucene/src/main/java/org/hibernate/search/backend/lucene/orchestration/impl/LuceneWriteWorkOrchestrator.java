/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneSchemaManagementWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;


public interface LuceneWriteWorkOrchestrator {

	default <T> CompletableFuture<T> submit(LuceneSchemaManagementWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneSchemaManagementWorkSet<>( work, future ) );
		return future;
	}

	default <T> CompletableFuture<T> submit(LuceneWriteWork<T> work,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneSingleWriteWorkSet<>( work, future, commitStrategy, refreshStrategy ) );
		return future;
	}

	void submit(LuceneWriteWorkSet workSet);

}
