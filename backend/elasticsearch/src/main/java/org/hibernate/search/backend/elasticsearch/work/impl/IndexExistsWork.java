/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexExistsWorkBuilder;


public class IndexExistsWork extends AbstractSimpleElasticsearchWork<Boolean> {

	private static final ElasticsearchRequestSuccessAssessor RESULT_ASSESSOR =
			DefaultElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	protected IndexExistsWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Boolean generateResult(ElasticsearchWorkExecutionContext context,
			ElasticsearchResponse response) {
		return ElasticsearchClientUtils.isSuccessCode( response.getStatusCode() );
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements IndexExistsWorkBuilder {
		private final URLEncodedString indexName;
		private final Boolean includeTypeName;

		public static Builder forElasticsearch66AndBelow(URLEncodedString indexName) {
			return new Builder( indexName, null );
		}

		public static Builder forElasticsearch67(URLEncodedString indexName) {
			/*
			 * We get warnings from Elasticsearch if we don't set include_type_name, even though it's just a HEAD request
			 * and we don't include any type name.
			 * It's probably a bug in ES, but it's gone in 7.0 so let's not bother with fixing it and let's just comply.
			 */
			return new Builder( indexName, false );
		}

		public static Builder forElasticsearch7AndAbove(URLEncodedString indexName) {
			return new Builder( indexName, null );
		}

		private Builder(URLEncodedString indexName, Boolean includeTypeName) {
			super( null, RESULT_ASSESSOR );
			this.indexName = indexName;
			this.includeTypeName = includeTypeName;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.head()
					.pathComponent( indexName );
			// ES6.7 only
			if ( includeTypeName != null ) {
				builder.param( "include_type_name", includeTypeName );
			}
			return builder.build();
		}

		@Override
		public IndexExistsWork build() {
			return new IndexExistsWork( this );
		}
	}
}