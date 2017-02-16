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
import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchRequestUtils;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public abstract class SimpleElasticsearchWork<J extends JestResult, R> implements ElasticsearchWork<R> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	protected final Action<J> action;
	private final LuceneWork luceneWork;
	protected final String dirtiedIndexName;
	protected final IndexingMonitor indexingMonitor;
	protected final ElasticsearchRequestSuccessAssessor<? super J> resultAssessor;
	protected final ElasticsearchWorkSuccessReporter<? super J> successReporter;
	protected final boolean markIndexDirty;

	protected SimpleElasticsearchWork(Builder<?, J> builder) {
		this.action = builder.buildAction();
		this.luceneWork = builder.luceneWork;
		this.dirtiedIndexName = builder.dirtiedIndexName;
		this.resultAssessor = builder.resultAssessor;
		this.indexingMonitor = builder.monitor;
		this.successReporter = builder.successReporter;
		this.markIndexDirty = builder.markIndexDirty;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "[" )
				.append( "action = " ).append( action )
				.append( ", dirtiedIndexName = " ).append( dirtiedIndexName )
				.append( "]" )
				.toString();
	}

	@Override
	public final R execute(ElasticsearchWorkExecutionContext executionContext) {
		J response;
		try {
			beforeExecute( executionContext );
			response = executionContext.getClient().executeRequest( action );
		}
		catch (IOException e) {
			GsonService gsonService = executionContext.getGsonService();
			throw LOG.elasticsearchRequestFailed(
					ElasticsearchRequestUtils.formatRequest( gsonService, action ),
					null, e );
		}

		resultAssessor.checkSuccess( executionContext, action, response );

		if ( indexingMonitor != null ) {
			IndexingMonitor bufferedIndexingMonitor = executionContext.getBufferedIndexingMonitor( indexingMonitor );
			successReporter.report( response, bufferedIndexingMonitor );
		}

		if ( markIndexDirty ) {
			executionContext.setIndexDirty( dirtiedIndexName );
		}

		return generateResult( executionContext, response );
	}

	protected void beforeExecute(ElasticsearchWorkExecutionContext executionContext) {
		// Do nothing by default
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, J response);

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

	@SuppressWarnings("unchecked") // By contract, subclasses must implement B
	protected abstract static class Builder<B, J extends JestResult> {
		protected final String dirtiedIndexName;
		protected ElasticsearchRequestSuccessAssessor<? super J> resultAssessor;
		protected final ElasticsearchWorkSuccessReporter<? super J> successReporter;

		protected LuceneWork luceneWork;
		protected IndexingMonitor monitor;
		protected boolean markIndexDirty;

		public Builder(String dirtiedIndexName,
				ElasticsearchRequestSuccessAssessor<? super J> resultAssessor,
				ElasticsearchWorkSuccessReporter<? super J> successReporter) {
			this.dirtiedIndexName = dirtiedIndexName;
			this.resultAssessor = resultAssessor;
			this.successReporter = successReporter;
		}

		public B luceneWork(LuceneWork luceneWork) {
			this.luceneWork = luceneWork;
			return (B) this;
		}

		public B monitor(IndexingMonitor monitor) {
			this.monitor = monitor;
			return (B) this;
		}

		public B markIndexDirty(boolean markIndexDirty) {
			this.markIndexDirty = markIndexDirty;
			return (B) this;
		}

		protected abstract Action<J> buildAction();

		public abstract ElasticsearchWork<?> build();
	}
}
