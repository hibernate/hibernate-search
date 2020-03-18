/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ScrollWorkBuilder;

import com.google.gson.JsonObject;


public class ScrollWork<R> extends AbstractNonBulkableWork<R> {

	private final ElasticsearchSearchResultExtractor<R> resultExtractor;

	protected ScrollWork(Builder<R> builder) {
		super( builder );
		this.resultExtractor = builder.resultExtractor;
	}

	@Override
	protected R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.getBody();
		return resultExtractor.extract( body );
	}

	public static class Builder<R>
			extends AbstractBuilder<Builder<R>>
			implements ScrollWorkBuilder<R> {
		private final String scrollId;
		private final String scrollTimeout;
		private final ElasticsearchSearchResultExtractor<R> resultExtractor;

		public Builder(String scrollId, String scrollTimeout, ElasticsearchSearchResultExtractor<R> resultExtractor) {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.scrollId = scrollId;
			this.scrollTimeout = scrollTimeout;
			this.resultExtractor = resultExtractor;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			JsonObject body = new JsonObject();
			body.addProperty( "scroll_id", scrollId );
			body.addProperty( "scroll", scrollTimeout );

			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.pathComponent( Paths._SEARCH )
					.pathComponent( Paths.SCROLL )
					.body( body );

			return builder.build();
		}

		@Override
		public ScrollWork<R> build() {
			return new ScrollWork<>( this );
		}
	}
}