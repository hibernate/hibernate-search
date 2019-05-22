/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;

/**
 * An thread-unsafe component responsible for accumulating works to be executed,
 * then executing them according to an implementation-specific orchestration scheme.
 * <p>
 * Works are added through the {@link #submit(ElasticsearchWork)} or {@link #submit(List)} methods,
 * and executed through the {@link #endBatch()} method.
 * <p>
 * Depending on the implementation, works may be executed serially, or in parallel.
 */
interface ElasticsearchWorkProcessor extends BatchingExecutor.WorkProcessor {

	<T> CompletableFuture<T> submit(ElasticsearchWork<T> work);

	CompletableFuture<?> submit(List<ElasticsearchWork<?>> work);

}
