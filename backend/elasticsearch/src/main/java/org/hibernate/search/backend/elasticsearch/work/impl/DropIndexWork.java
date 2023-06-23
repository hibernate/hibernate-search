/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public class DropIndexWork extends AbstractNonBulkableWork<Void> {

	protected DropIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder> {
		private final URLEncodedString indexName;

		public Builder(URLEncodedString indexName) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.indexName = indexName;
		}

		public Builder ignoreIndexNotFound() {
			this.resultAssessor = ElasticsearchRequestSuccessAssessor.builder()
					.ignoreErrorTypes( "index_not_found_exception" )
					.build();

			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.delete()
							.pathComponent( indexName );

			return builder.build();
		}

		@Override
		public DropIndexWork build() {
			return new DropIndexWork( this );
		}
	}
}
