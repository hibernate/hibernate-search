/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.HashSet;
import java.util.Set;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;

import com.google.gson.JsonObject;

/**
 * A delete by query work for ES5, using the core delete-by-query API.
 *
 * @author Yoann Rodiere
 */
public class ES5DeleteByQueryWork extends SimpleElasticsearchWork<Void> {

	protected ES5DeleteByQueryWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements DeleteByQueryWorkBuilder {
		private final URLEncodedString indexName;
		private final JsonObject payload;
		private final Set<URLEncodedString> typeNames = new HashSet<>();

		public Builder(URLEncodedString indexName, JsonObject payload) {
			super( indexName, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.payload = payload;
		}

		@Override
		public Builder type(URLEncodedString typeName) {
			typeNames.add( typeName );
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.pathComponent( indexName );

			if ( !typeNames.isEmpty() ) {
				builder.multiValuedPathComponent( typeNames );
			}

			builder.pathComponent( Paths._DELETE_BY_QUERY )
					.body( payload );

			return builder.build();
		}

		@Override
		public ES5DeleteByQueryWork build() {
			return new ES5DeleteByQueryWork( this );
		}
	}
}