/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.common.timing.Deadline;

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

	public static class Builder extends AbstractBuilder<Builder> {

		private final List<URLEncodedString> indexNames = new ArrayList<>();
		private JsonObject query;
		private Set<String> routingKeys;
		private Deadline deadline;

		public Builder() {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
		}

		public Builder index(URLEncodedString indexName) {
			indexNames.add( indexName );
			return this;
		}

		public Builder query(JsonObject query) {
			this.query = query;
			return this;
		}

		public Builder routingKeys(Set<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
		}

		public Builder deadline(Deadline deadline) {
			this.deadline = deadline;
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

			if ( deadline != null ) {
				builder.deadline( deadline );
			}

			return builder.build();
		}

		@Override
		public CountWork build() {
			return new CountWork( this );
		}
	}
}
