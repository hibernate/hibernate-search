/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.real.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexExistsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.real.accessor.impl.DefaultElasticsearchRequestSuccessAssessor;
import org.hibernate.search.backend.elasticsearch.work.real.accessor.impl.ElasticsearchRequestSuccessAssessor;

/**
 * @author Yoann Rodiere
 */
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
			extends AbstractSimpleElasticsearchWork.Builder<Builder>
			implements IndexExistsWorkBuilder {
		private final URLEncodedString indexName;

		public Builder(URLEncodedString indexName) {
			super( null, RESULT_ASSESSOR );
			this.indexName = indexName;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.head()
					.pathComponent( indexName );
			return builder.build();
		}

		@Override
		public IndexExistsWork build() {
			return new IndexExistsWork( this );
		}
	}
}