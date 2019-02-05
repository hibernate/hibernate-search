/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.WaitForIndexStatusWorkBuilder;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class WaitForIndexStatusWork extends AbstractSimpleElasticsearchWork<Void> {

	protected WaitForIndexStatusWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder>
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

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
				throw log.unexpectedIndexStatus( indexName.original, requiredIndexStatus.getElasticsearchString(), status, timeoutAndUnit );
			}
		}

		@Override
		public void checkSuccess(JsonObject bulkResponseItem) {
			throw new AssertionFailure( "This method should never be called, because WaitForIndexStatusWork is not bulkable." );
		}

	}
}
