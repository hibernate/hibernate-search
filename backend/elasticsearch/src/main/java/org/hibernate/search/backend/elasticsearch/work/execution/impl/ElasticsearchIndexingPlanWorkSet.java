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
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentElasticsearchWork;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;

class ElasticsearchIndexingPlanWorkSet implements ElasticsearchWorkSet {
	private final List<SingleDocumentElasticsearchWork<?>> works;
	private final CompletableFuture<IndexIndexingPlanExecutionReport> indexingPlanFuture;

	ElasticsearchIndexingPlanWorkSet(List<SingleDocumentElasticsearchWork<?>> works,
			CompletableFuture<IndexIndexingPlanExecutionReport> indexingPlanFuture) {
		this.works = new ArrayList<>( works );
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

	private IndexIndexingPlanExecutionReport buildReport(CompletableFuture<?>[] finishedWorkFutures, Throwable throwable) {
		IndexIndexingPlanExecutionReport.Builder reportBuilder = IndexIndexingPlanExecutionReport.builder();
		for ( int i = 0; i < finishedWorkFutures.length; i++ ) {
			CompletableFuture<?> future = finishedWorkFutures[i];
			if ( future.isCompletedExceptionally() ) {
				reportBuilder.throwable( Futures.getThrowableNow( future ) );
				SingleDocumentElasticsearchWork<?> work = works.get( i );
				reportBuilder.failingDocument( work.getDocumentReference() );
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
