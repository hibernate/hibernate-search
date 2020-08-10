/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.timeout.impl.ElasticsearchTimeoutManager;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CountWorkBuilder;

import com.google.gson.JsonObject;

public class CountWork extends AbstractNonBulkableWork<Long> {

	private static final JsonAccessor<Long> COUNT_ACCESSOR = JsonAccessor.root().property( "count" ).asLong();

	protected CountWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Long generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.body();
		return COUNT_ACCESSOR.get( body ).get();
	}

	public static class Builder extends AbstractBuilder<Builder> implements CountWorkBuilder {

		private final List<URLEncodedString> indexNames = new ArrayList<>();
		private JsonObject query;
		private Set<String> routingKeys;
		private ElasticsearchTimeoutManager timeoutManager;

		public Builder() {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
		}

		@Override
		public Builder index(URLEncodedString indexName) {
			indexNames.add( indexName );
			return this;
		}

		@Override
		public Builder query(JsonObject query) {
			this.query = query;
			return this;
		}

		@Override
		public Builder routingKeys(Set<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
		}

		@Override
		public CountWorkBuilder timeout(ElasticsearchTimeoutManager timeoutManager) {
			this.timeoutManager = timeoutManager;
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
							.multiValuedPathComponent( indexNames );

			builder.pathComponent( Paths._COUNT );

			if ( query != null ) {
				builder.body( query );
			}

			if ( !routingKeys.isEmpty() ) {
				builder.multiValuedParam( "routing", routingKeys );
			}

			if ( timeoutManager.exceptionOnTimeout() ) {
				// set timeoutValue and timeoutUnit only for hard timeout
				builder.timeout( timeoutManager.timeoutValue(), timeoutManager.timeoutUnit() );
			}

			return builder.build();
		}

		@Override
		public CountWork build() {
			return new CountWork( this );
		}
	}
}
