/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;


public interface LuceneWriteWorkOrchestrator {

	<T> CompletableFuture<T> submit(LuceneWriteWork<T> work,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	CompletableFuture<?> submit(List<LuceneWriteWork<?>> work,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

}
