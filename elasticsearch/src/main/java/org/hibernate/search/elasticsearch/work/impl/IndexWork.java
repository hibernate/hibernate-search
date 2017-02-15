/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import com.google.gson.JsonObject;

import io.searchbox.action.BulkableAction;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;

/**
 * @author Yoann Rodiere
 */
public class IndexWork extends SimpleBulkableElasticsearchWork<DocumentResult> {

	public IndexWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleBulkableElasticsearchWork.Builder<Builder, DocumentResult> {
		private final Index.Builder jestBuilder;

		public Builder(String indexName, String typeName, String id, JsonObject document) {
			super( indexName, DefaultElasticsearchRequestResultAssessor.INSTANCE, DocumentAddedElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new Index.Builder( document )
					.index( indexName )
					.type( typeName )
					.id( id );
		}

		@Override
		protected BulkableAction<DocumentResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public IndexWork build() {
			return new IndexWork( this );
		}
	}
}