/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.WaitForIndexStatusWorkBuilder;


public class WaitForIndexStatusWork extends AbstractNonBulkableWork<Void> {

	protected WaitForIndexStatusWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements WaitForIndexStatusWorkBuilder {
		private final URLEncodedString indexName;
		private final IndexStatus requiredStatus;
		private final String timeout;

		public Builder(URLEncodedString indexName, IndexStatus requiredStatus, String timeout) {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.requiredStatus = requiredStatus;
			this.timeout = timeout;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
					.pathComponent( Paths._CLUSTER )
					.pathComponent( Paths.HEALTH )
					.pathComponent( indexName )
					.param( "wait_for_status", requiredStatus.externalRepresentation() )
					.param( "timeout", timeout );

			return builder.build();
		}

		@Override
		public WaitForIndexStatusWork build() {
			return new WaitForIndexStatusWork( this );
		}
	}
}
