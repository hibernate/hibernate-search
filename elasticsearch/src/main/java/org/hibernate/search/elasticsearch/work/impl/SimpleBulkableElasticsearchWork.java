/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;

import io.searchbox.action.BulkableAction;
import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * @author Yoann Rodiere
 */
public class SimpleBulkableElasticsearchWork<T extends JestResult>
		extends SimpleElasticsearchWork<T>
		implements BulkableElasticsearchWork<T> {

	public SimpleBulkableElasticsearchWork(BulkableAction<T> action,
			LuceneWork luceneWork,
			String indexName,
			ElasticsearchRequestResultAssessor<? super T> resultAssessor,
			IndexingMonitor indexingMonitor,
			ElasticsearchWorkSuccessReporter<? super T> successReporter,
			boolean markIndexDirty) {
		super( action, luceneWork, indexName, resultAssessor, indexingMonitor, successReporter, markIndexDirty );
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
		if ( resultAssessor.isSuccess( resultItem ) ) {
			if ( indexingMonitor != null ) {
				IndexingMonitor bufferedIndexingMonitor = context.getBufferedIndexingMonitor( indexingMonitor );
				successReporter.report( resultItem, bufferedIndexingMonitor );
			}

			if ( markIndexDirty ) {
				context.setIndexDirty( indexName );
			}

			return true;
		}
		else {
			return false;
		}
	}

}
