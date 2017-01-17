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
import org.hibernate.search.elasticsearch.work.impl.builder.FlushWorkBuilder;

import com.google.gson.JsonObject;

/**
 * A flush work for ES2, using the Flush API.
 *
 * @author Yoann Rodiere
 */
public class ES2FlushWork extends SimpleElasticsearchWork<Void> {

	protected ES2FlushWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements FlushWorkBuilder {
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
					ElasticsearchRequest.post()
					.param( "refresh", true );

			if ( !indexNames.isEmpty() ) {
				builder.multiValuedPathComponent( indexNames );
			}

			builder.pathComponent( "_flush" );

			return builder.build();
		}

		@Override
		public ES2FlushWork build() {
			return new ES2FlushWork( this );
		}
	}
}