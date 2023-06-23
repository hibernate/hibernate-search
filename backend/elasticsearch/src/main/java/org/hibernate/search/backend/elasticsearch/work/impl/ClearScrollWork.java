/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;

import com.google.gson.JsonObject;

public class ClearScrollWork extends AbstractNonBulkableWork<Void> {

	protected ClearScrollWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder> {
		private final String scrollId;

		public Builder(String scrollId) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.scrollId = scrollId;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			JsonObject body = new JsonObject();
			body.addProperty( "scroll_id", scrollId );

			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.delete()
							.pathComponent( Paths._SEARCH )
							.pathComponent( Paths.SCROLL )
							.body( body );

			return builder.build();
		}

		@Override
		public ClearScrollWork build() {
			return new ClearScrollWork( this );
		}
	}
}
