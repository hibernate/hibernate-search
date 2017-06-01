/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.ScrollWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ScrollWork extends SimpleElasticsearchWork<SearchResult> {

	protected ScrollWork(Builder builder) {
		super( builder );
	}

	@Override
	protected SearchResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.getBody();
		return new SearchWork.SearchResultImpl( body );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements ScrollWorkBuilder {
		private final String scrollId;
		private final String scrollTimeout;

		public Builder(String scrollId, String scrollTimeout) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.scrollId = scrollId;
			this.scrollTimeout = scrollTimeout;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.pathComponent( Paths._SEARCH )
					.pathComponent( Paths.SCROLL )
					.body(JsonBuilder.object()
							.addProperty( "scroll_id", scrollId )
							.addProperty( "scroll", scrollTimeout )
							.build()
					);

			return builder.build();
		}

		@Override
		public ScrollWork build() {
			return new ScrollWork( this );
		}
	}
}