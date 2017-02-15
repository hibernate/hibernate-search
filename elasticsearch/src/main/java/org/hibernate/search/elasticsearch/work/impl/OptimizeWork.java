/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.Optimize;

/**
 * @author Yoann Rodiere
 */
public class OptimizeWork extends SimpleElasticsearchWork<JestResult> {

	protected OptimizeWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final Optimize.Builder jestBuilder;

		public Builder() {
			super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			/*
			 * As of ES 2.1, the Optimize API has been renamed to ForceMerge,
			 * but Jest still does not provide commands for the ForceMerge API as of
			 * version 2.0.3
			 * See https://github.com/searchbox-io/Jest/issues/292
			 */
			this.jestBuilder = new Optimize.Builder();
		}

		public Builder index(String indexName) {
			jestBuilder.addIndex( indexName );
			return this;
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public OptimizeWork build() {
			return new OptimizeWork( this );
		}
	}
}