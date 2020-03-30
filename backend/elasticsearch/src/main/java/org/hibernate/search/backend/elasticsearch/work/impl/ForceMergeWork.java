/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.MergeSegmentsWorkBuilder;

/**
 * A force-merge work for ES5+.
 * <p>
 * The ForceMerge API replaced the removed Optimize API in ES5.
 */
public class ForceMergeWork extends AbstractNonBulkableElasticsearchWork<Void> {

	protected ForceMergeWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements MergeSegmentsWorkBuilder {
		private final List<URLEncodedString> indexNames = new ArrayList<>();

		public Builder() {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
		}

		@Override
		public Builder index(URLEncodedString indexName) {
			this.indexNames.add( indexName );
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post();

			if ( !indexNames.isEmpty() ) {
				builder.multiValuedPathComponent( indexNames );
			}

			builder.pathComponent( Paths._FORCEMERGE );

			return builder.build();
		}

		@Override
		public ForceMergeWork build() {
			return new ForceMergeWork( this );
		}
	}
}