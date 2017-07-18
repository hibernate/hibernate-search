/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.builder.WaitForIndexStatusWorkBuilder;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class WaitForIndexStatusWork extends SimpleElasticsearchWork<Void> {

	protected WaitForIndexStatusWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements WaitForIndexStatusWorkBuilder {
		private final URLEncodedString indexName;
		private final ElasticsearchIndexStatus requiredStatus;
		private final String timeout;

		public Builder(URLEncodedString indexName, ElasticsearchIndexStatus requiredStatus, String timeout) {
			super( null, new SuccessAssessor( indexName, requiredStatus, timeout ) );
			this.indexName = indexName;
			this.requiredStatus = requiredStatus;
			this.timeout = timeout;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
					.pathComponent( Paths._CLUSTER )
					.pathComponent( Paths.HEALTH )
					.pathComponent( indexName )
					.param( "wait_for_status", requiredStatus.getElasticsearchString() )
					.param( "timeout", timeout );

			return builder.build();
		}

		@Override
		public WaitForIndexStatusWork build() {
			return new WaitForIndexStatusWork( this );
		}
	}

	private static class SuccessAssessor implements ElasticsearchRequestSuccessAssessor {

		private static final Log LOG = LoggerFactory.make( Log.class );

		private static final int TIMED_OUT_HTTP_STATUS_CODE = 408;

		private final URLEncodedString indexName;

		private final ElasticsearchIndexStatus requiredIndexStatus;

		private final String timeoutAndUnit;

		private final DefaultElasticsearchRequestSuccessAssessor delegate;

		public SuccessAssessor(URLEncodedString indexName,
				ElasticsearchIndexStatus requiredIndexStatus,
				String timeoutAndUnit) {
			super();
			this.indexName = indexName;
			this.requiredIndexStatus = requiredIndexStatus;
			this.timeoutAndUnit = timeoutAndUnit;
			this.delegate = DefaultElasticsearchRequestSuccessAssessor.builder( )
					.ignoreErrorStatuses( TIMED_OUT_HTTP_STATUS_CODE ).build();
		}

		@Override
		public void checkSuccess(ElasticsearchResponse response) throws SearchException {
			this.delegate.checkSuccess( response );
			if ( response.getStatusCode() == TIMED_OUT_HTTP_STATUS_CODE ) {
				String status = response.getBody().get( "status" ).getAsString();
				throw LOG.unexpectedIndexStatus( indexName.original, requiredIndexStatus.getElasticsearchString(), status, timeoutAndUnit );
			}
		}

		@Override
		public void checkSuccess(JsonObject bulkResponseItem) {
			throw new AssertionFailure( "This method should never be called, because WaitForIndexStatusWork is not bulkable." );
		}

	}
}