/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.io.IOException;
import java.util.stream.Stream;

import org.elasticsearch.client.Response;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public abstract class SimpleElasticsearchWork<R> implements ElasticsearchWork<R> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	protected final ElasticsearchRequest request;
	private final LuceneWork luceneWork;
	protected final String dirtiedIndexName;
	protected final ElasticsearchRequestSuccessAssessor resultAssessor;
	protected final boolean markIndexDirty;

	protected SimpleElasticsearchWork(Builder<?> builder) {
		this.request = builder.buildRequest();
		this.luceneWork = builder.luceneWork;
		this.dirtiedIndexName = builder.dirtiedIndexName;
		this.resultAssessor = builder.resultAssessor;
		this.markIndexDirty = builder.markIndexDirty;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "[" )
				.append( "path = " ).append( request.getPath() )
				.append( ", dirtiedIndexName = " ).append( dirtiedIndexName )
				.append( "]" )
				.toString();
	}

	@Override
	public final R execute(ElasticsearchWorkExecutionContext executionContext) {
		GsonProvider gsonProvider = executionContext.getGsonProvider();
		Response response = null;
		JsonObject parsedResponseBody = null;
		try {
			beforeExecute( executionContext, request );
			response = executionContext.getClient().execute( request );
			parsedResponseBody = ElasticsearchClientUtils.parseJsonResponse( gsonProvider, response );
		}
		catch (IOException | RuntimeException e) {
			throw LOG.elasticsearchRequestFailed(
					ElasticsearchClientUtils.formatRequest( gsonProvider, request ),
					ElasticsearchClientUtils.formatResponse( gsonProvider, response, parsedResponseBody ),
					e );
		}

		resultAssessor.checkSuccess( executionContext, request, response, parsedResponseBody );

		afterSuccess( executionContext );

		if ( markIndexDirty ) {
			executionContext.setIndexDirty( dirtiedIndexName );
		}

		return generateResult( executionContext, response, parsedResponseBody );
	}

	protected void beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		// Do nothing by default
	}

	protected void afterSuccess(ElasticsearchWorkExecutionContext executionContext) {
		// Do nothing by default
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody);

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
	protected abstract static class Builder<B> {
		protected final String dirtiedIndexName;
		protected ElasticsearchRequestSuccessAssessor resultAssessor;

		protected LuceneWork luceneWork;
		protected boolean markIndexDirty;

		public Builder(String dirtiedIndexName, ElasticsearchRequestSuccessAssessor resultAssessor) {
			this.dirtiedIndexName = dirtiedIndexName;
			this.resultAssessor = resultAssessor;
		}

		public B luceneWork(LuceneWork luceneWork) {
			this.luceneWork = luceneWork;
			return (B) this;
		}

		public B markIndexDirty(boolean markIndexDirty) {
			this.markIndexDirty = markIndexDirty;
			return (B) this;
		}

		protected abstract ElasticsearchRequest buildRequest();

		public abstract ElasticsearchWork<?> build();
	}
}
