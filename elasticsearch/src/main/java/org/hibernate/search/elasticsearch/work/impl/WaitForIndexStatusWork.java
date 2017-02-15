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
import org.hibernate.search.exception.AssertionFailure;

import io.searchbox.action.AbstractAction;
import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;

/**
 * @author Yoann Rodiere
 */
public class WaitForIndexStatusWork extends SimpleElasticsearchWork<JestResult> {

	private static final ElasticsearchRequestResultAssessor<? super JestResult> RESULT_ASSESSOR =
			DefaultElasticsearchRequestResultAssessor.builder().ignoreErrorStatuses( 408 ).build();

	protected WaitForIndexStatusWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final Health.Builder jestBuilder;
		private final String indexName;

		public Builder(String indexName, ElasticsearchIndexStatus requiredStatus, String timeout) {
			super( null, RESULT_ASSESSOR, NoopElasticsearchWorkSuccessReporter.INSTANCE );
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
}