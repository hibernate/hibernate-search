/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.util.common.impl.Futures;

/**
 * An abstract base for {@link ElasticsearchWorkOrchestratorImplementor} implementations.
 */
abstract class AbstractElasticsearchWorkOrchestrator
		extends AbstractWorkOrchestrator<AbstractElasticsearchWorkOrchestrator.ElasticsearchWorkSet>
		implements ElasticsearchWorkOrchestratorImplementor {

	AbstractElasticsearchWorkOrchestrator(String name) {
		super( name );
	}

	@Override
	public CompletableFuture<?> submit(List<ElasticsearchWork<?>> works) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		submit( new ElasticsearchMultipleWorkSet( works, future ) );
		return future;
	}

	@Override
	public <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new ElasticsearchSingleWorkSet<>( work, future ) );
		return future;
	}

	interface ElasticsearchWorkSet extends BatchingExecutor.Task<ElasticsearchWorkOrchestrationStrategy> {
	}

	static class ElasticsearchMultipleWorkSet implements ElasticsearchWorkSet {
		private final List<ElasticsearchWork<?>> works;
		private final CompletableFuture<Object> future;

		ElasticsearchMultipleWorkSet(List<ElasticsearchWork<?>> works, CompletableFuture<Object> future) {
			this.works = new ArrayList<>( works );
			this.future = future;
		}

		@Override
		public void submitTo(ElasticsearchWorkOrchestrationStrategy delegate) {
			delegate.submit( works ).whenComplete( Futures.copyHandler( future ) );
		}

		@Override
		public void markAsFailed(Throwable t) {
			future.completeExceptionally( t );
		}
	}

	static class ElasticsearchSingleWorkSet<T> implements ElasticsearchWorkSet {
		private final ElasticsearchWork<T> work;
		private final CompletableFuture<T> future;

		ElasticsearchSingleWorkSet(ElasticsearchWork<T> work, CompletableFuture<T> future) {
			this.work = work;
			this.future = future;
		}

		@Override
		public void submitTo(ElasticsearchWorkOrchestrationStrategy delegate) {
			delegate.submit( work ).whenComplete( Futures.copyHandler( future ) );
		}

		@Override
		public void markAsFailed(Throwable t) {
			future.completeExceptionally( t );
		}
	}

}
