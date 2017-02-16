/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.builder.WaitForIndexStatusWorkBuilder;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.AbstractAction;
import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * @author Yoann Rodiere
 */
public class WaitForIndexStatusWork extends SimpleElasticsearchWork<JestResult, Void> {

	protected WaitForIndexStatusWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult>
			implements WaitForIndexStatusWorkBuilder {
		private final Health.Builder jestBuilder;
		private final String indexName;

		public Builder(String indexName, ElasticsearchIndexStatus requiredStatus, String timeout) {
			super( null, new SuccessAssessor( indexName, requiredStatus, timeout ), NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.indexName = indexName;
			this.jestBuilder = new Health.Builder()
					.setParameter( "wait_for_status", requiredStatus.getElasticsearchString() )
					.setParameter( "timeout", timeout );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return new IndexHealth( jestBuilder, indexName );
		}

		@Override
		public WaitForIndexStatusWork build() {
			return new WaitForIndexStatusWork( this );
		}
	}

	private static class IndexHealth extends Health {
		public IndexHealth(Builder builder, String indexName) {
			super( builder );
			try {
				/*
				 * Hack: there is an indexName in a super class, but we can't change it before buildURI() is called.
				 * So we have to re-compute the URI after it has been set...
				 */
				setURI( buildURI() + URLEncoder.encode( indexName, AbstractAction.CHARSET ) );
			}
			catch (UnsupportedEncodingException e) {
				throw new AssertionFailure( "Unexpectedly unsupported charset", e );
			}
		}
	}

	private static class SuccessAssessor implements ElasticsearchRequestSuccessAssessor<JestResult> {

		private static final Log LOG = LoggerFactory.make( Log.class );

		private static final int TIMED_OUT_HTTP_STATUS_CODE = 408;

		private final String indexName;

		private final ElasticsearchIndexStatus requiredIndexStatus;

		private final String timeoutAndUnit;

		private final DefaultElasticsearchRequestSuccessAssessor delegate;

		public SuccessAssessor(String indexName,
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
		public void checkSuccess(ElasticsearchWorkExecutionContext context, Action<? extends JestResult> request, JestResult result) throws SearchException {
			this.delegate.checkSuccess( context, request, result );
			if ( result.getResponseCode() == TIMED_OUT_HTTP_STATUS_CODE ) {
				String status = result.getJsonObject().get( "status" ).getAsString();
				throw LOG.unexpectedIndexStatus( indexName, requiredIndexStatus.getElasticsearchString(), status, timeoutAndUnit );
			}
		}

		@Override
		public boolean isSuccess(ElasticsearchWorkExecutionContext context, BulkResultItem bulkResultItem) {
			throw new AssertionFailure( "This method should never be called, because WaitForIndexStatus actions are not Bulkable" );
		}

	}
}