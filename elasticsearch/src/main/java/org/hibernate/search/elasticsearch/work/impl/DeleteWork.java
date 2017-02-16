/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import io.searchbox.action.BulkableAction;
import io.searchbox.client.JestResult;
import io.searchbox.core.Delete;
import io.searchbox.core.DocumentResult;

/**
 * @author Yoann Rodiere
 */
public class DeleteWork extends SimpleBulkableElasticsearchWork<DocumentResult, Void> {

	private static final ElasticsearchRequestSuccessAssessor<JestResult> RESULT_ASSESSOR =
			DefaultElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	public DeleteWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, DocumentResult response) {
		return null;
	}

	public static class Builder
			extends SimpleBulkableElasticsearchWork.Builder<Builder, DocumentResult> {
		private final Delete.Builder jestBuilder;

		public Builder(String indexName, String typeName, String id) {
			super( indexName, RESULT_ASSESSOR, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new Delete.Builder( id )
					.index( indexName )
					.type( typeName );
		}

		@Override
		protected BulkableAction<DocumentResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public DeleteWork build() {
			return new DeleteWork( this );
		}
	}
}