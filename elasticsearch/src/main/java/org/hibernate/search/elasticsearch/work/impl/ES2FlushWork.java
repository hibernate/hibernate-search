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
		private final Set<URLEncodedString> indexNames = new HashSet<>();

		public Builder() {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
		}

		@Override
		public Builder index(URLEncodedString indexName) {
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

			builder.pathComponent( Paths._FLUSH );

			return builder.build();
		}

		@Override
		public ES2FlushWork build() {
			return new ES2FlushWork( this );
		}
	}
}