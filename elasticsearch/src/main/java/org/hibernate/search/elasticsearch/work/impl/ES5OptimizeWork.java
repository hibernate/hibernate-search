/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.work.impl.builder.OptimizeWorkBuilder;

import com.google.gson.JsonObject;

/**
 * An optimize work for ES5, using the ForceMerge API.
 * <p>
 * The ForceMerge API replaces the removed Optimize API in ES5.
 *
 * @author Yoann Rodiere
 */
public class ES5OptimizeWork extends SimpleElasticsearchWork<Void> {

	protected ES5OptimizeWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements OptimizeWorkBuilder {
		private List<String> indexNames = new ArrayList<>();

		public Builder() {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
		}

		@Override
		public Builder index(String indexName) {
			this.indexNames.add( indexName );
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post();

			if ( !indexNames.isEmpty() ) {
				builder.multiValuedPathComponent( indexNames );
			}

			builder.pathComponent( "_forcemerge" );

			return builder.build();
		}

		@Override
		public ES5OptimizeWork build() {
			return new ES5OptimizeWork( this );
		}
	}
}