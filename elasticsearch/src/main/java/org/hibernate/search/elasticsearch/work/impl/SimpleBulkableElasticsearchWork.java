/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.backend.IndexingMonitor;

import io.searchbox.action.BulkableAction;
import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * @author Yoann Rodiere
 */
public class SimpleBulkableElasticsearchWork<R extends JestResult>
		extends SimpleElasticsearchWork<R>
		implements BulkableElasticsearchWork<R> {

	protected SimpleBulkableElasticsearchWork(Builder<?, R> builder) {
		super( builder );
	}

	@Override
	public void aggregate(ElasticsearchWorkAggregator aggregator) {
		aggregator.addBulkable( this );
	}

	@Override
	public BulkableAction<?> getBulkableAction() {
		return (BulkableAction<?>) action;
	}

	@Override
	public boolean handleBulkResult(ElasticsearchWorkExecutionContext context, BulkResultItem resultItem) {
		if ( resultAssessor.isSuccess( context, resultItem ) ) {
			if ( indexingMonitor != null ) {
				IndexingMonitor bufferedIndexingMonitor = context.getBufferedIndexingMonitor( indexingMonitor );
				successReporter.report( resultItem, bufferedIndexingMonitor );
			}

			if ( markIndexDirty ) {
				context.setIndexDirty( dirtiedIndexName );
			}

			return true;
		}
		else {
			return false;
		}
	}

	protected abstract static class Builder<B, R extends JestResult>
			extends SimpleElasticsearchWork.Builder<B, R> {

		public Builder(String dirtiedIndexName,
				ElasticsearchRequestResultAssessor<? super R> resultAssessor,
				ElasticsearchWorkSuccessReporter<? super R> successReporter) {
			super( dirtiedIndexName, resultAssessor, successReporter );
		}

	}
}
