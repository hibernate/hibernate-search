/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.IndicesExists;

/**
 * @author Yoann Rodiere
 */
public class IndexExistsWork extends SimpleElasticsearchWork<JestResult, Boolean> {

	private static final ElasticsearchRequestSuccessAssessor<? super JestResult> RESULT_ASSESSOR =
			DefaultElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	protected IndexExistsWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Boolean generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		return response.getResponseCode() == 200;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final IndicesExists.Builder jestBuilder;

		public Builder(String indexName) {
			super( null, RESULT_ASSESSOR, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new IndicesExists.Builder( indexName );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public IndexExistsWork build() {
			return new IndexExistsWork( this );
		}
	}
}