/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.work.impl.builder.FlushWorkBuilder;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.Flush;

/**
 * @author Yoann Rodiere
 */
public class FlushWork extends SimpleElasticsearchWork<JestResult, Void> {

	protected FlushWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult>
			implements FlushWorkBuilder {
		private final Flush.Builder jestBuilder;

		public Builder() {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new Flush.Builder()
					.setParameter( "wait_if_ongoing", "true" )
					.refresh( true );
		}

		@Override
		public Builder index(String indexName) {
			jestBuilder.addIndex( indexName );
			return this;
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public FlushWork build() {
			return new FlushWork( this );
		}
	}
}