/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkSet;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;

class ElasticsearchIndexingPlanWorkSet<R> implements ElasticsearchWorkSet {
	private final List<SingleDocumentWork<?>> works;
	private final EntityReferenceFactory<R> entityReferenceFactory;
	private final CompletableFuture<IndexIndexingPlanExecutionReport<R>> indexingPlanFuture;

	ElasticsearchIndexingPlanWorkSet(List<SingleDocumentWork<?>> works,
			EntityReferenceFactory<R> entityReferenceFactory,
			CompletableFuture<IndexIndexingPlanExecutionReport<R>> indexingPlanFuture) {
		this.works = new ArrayList<>( works );
		this.entityReferenceFactory = entityReferenceFactory;
		this.indexingPlanFuture = indexingPlanFuture;
	}

	@Override
	public void submitTo(ElasticsearchWorkProcessor delegate) {
		delegate.beforeWorkSet();
		CompletableFuture<?>[] workFutures = new CompletableFuture[works.size()];
		for ( int i = 0; i < works.size(); i++ ) {
			workFutures[i] = delegate.submit( works.get( i ) );
		}
		delegate.afterWorkSet()
				.handle( Futures.handler( (result, throwable) -> {
					return buildReport( workFutures, throwable );
				} ) )
				.whenComplete( Futures.copyHandler( indexingPlanFuture ) );
	}

	private IndexIndexingPlanExecutionReport<R> buildReport(CompletableFuture<?>[] finishedWorkFutures, Throwable throwable) {
		IndexIndexingPlanExecutionReport.Builder<R> reportBuilder = IndexIndexingPlanExecutionReport.builder();
		for ( int i = 0; i < finishedWorkFutures.length; i++ ) {
			CompletableFuture<?> future = finishedWorkFutures[i];
			if ( future.isCompletedExceptionally() ) {
				reportBuilder.throwable( Futures.getThrowableNow( future ) );
				SingleDocumentWork<?> work = works.get( i );
				try {
					reportBuilder.failingEntityReference(
							entityReferenceFactory,
							work.getEntityTypeName(), work.getEntityIdentifier()
					);
				}
				catch (RuntimeException e) {
					reportBuilder.throwable( e );
				}
			}
		}
		if ( throwable != null ) {
			reportBuilder.throwable( throwable );
		}
		return reportBuilder.build();
	}

	@Override
	public void markAsFailed(Throwable t) {
		indexingPlanFuture.completeExceptionally( t );
	}
}
