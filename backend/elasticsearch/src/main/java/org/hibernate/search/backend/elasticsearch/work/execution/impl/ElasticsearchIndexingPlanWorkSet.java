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
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.util.common.impl.Futures;

class ElasticsearchIndexingPlanWorkSet implements ElasticsearchWorkSet {
	private final List<ElasticsearchWork<?>> works;
	private final CompletableFuture<Object> future;

	ElasticsearchIndexingPlanWorkSet(List<ElasticsearchWork<?>> works, CompletableFuture<Object> future) {
		this.works = new ArrayList<>( works );
		this.future = future;
	}

	@Override
	public void submitTo(ElasticsearchWorkProcessor delegate) {
		delegate.beforeWorkSet();
		for ( ElasticsearchWork<?> work : works ) {
			delegate.submit( work );
		}
		delegate.afterWorkSet().whenComplete( Futures.copyHandler( future ) );
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}
}
