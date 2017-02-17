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
import org.hibernate.search.elasticsearch.work.impl.builder.OptimizeWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class OptimizeWork extends SimpleElasticsearchWork<Void> {

	protected OptimizeWork(Builder builder) {
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

			builder.pathComponent( "_optimize" );

			return builder.build();
		}

		@Override
		public OptimizeWork build() {
			return new OptimizeWork( this );
		}
	}
}