/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.io.IOException;
import java.util.stream.Stream;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class SimpleElasticsearchWork<T extends JestResult> implements ElasticsearchWork<T> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	protected final Action<T> action;
	private final LuceneWork luceneWork;
	protected final String indexName;
	protected final IndexingMonitor indexingMonitor;
	protected final ElasticsearchRequestResultAssessor<? super T> resultAssessor;
	protected final ElasticsearchWorkSuccessReporter<? super T> successReporter;
	protected final boolean markIndexDirty;

	public SimpleElasticsearchWork(Action<T> action, LuceneWork luceneWork, String indexName,
			ElasticsearchRequestResultAssessor<? super T> resultAssessor,
			IndexingMonitor indexingMonitor, ElasticsearchWorkSuccessReporter<? super T> successReporter,
			boolean markIndexDirty) {
		this.action = action;
		this.luceneWork = luceneWork;
		this.indexName = indexName;
		this.resultAssessor = resultAssessor;
		this.indexingMonitor = indexingMonitor;
		this.successReporter = successReporter;
		this.markIndexDirty = markIndexDirty;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "[" )
				.append( "action = " ).append( action )
				.append( ", indexName = " ).append( indexName )
				.append( "]" )
				.toString();
	}

	@Override
	public T execute(ElasticsearchWorkExecutionContext executionContext) {
		T result;
		try {
			result = executionContext.getClient().executeRequest( action );
		}
		catch (IOException e) {
			throw LOG.elasticsearchRequestFailed( executionContext.getJestAPIFormatter().formatRequest( action ), null, e );
		}

		resultAssessor.checkSuccess( action, result );

		if ( indexingMonitor != null ) {
			IndexingMonitor bufferedIndexingMonitor = executionContext.getBufferedIndexingMonitor( indexingMonitor );
			successReporter.report( result, bufferedIndexingMonitor );
		}

		if ( markIndexDirty ) {
			executionContext.setIndexDirty( indexName );
		}

		return result;
	}

	@Override
	public void aggregate(ElasticsearchWorkAggregator aggregator) {
		// May be overridden by subclasses
		aggregator.addNonBulkable( this );
	}

	@Override
	public Stream<LuceneWork> getLuceneWorks() {
		if ( luceneWork != null ) {
			return Stream.of( luceneWork );
		}
		else {
			return Stream.empty();
		}
	}
}
