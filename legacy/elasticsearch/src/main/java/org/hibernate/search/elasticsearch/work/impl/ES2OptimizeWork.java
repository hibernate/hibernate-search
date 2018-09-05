/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.work.impl.builder.OptimizeWorkBuilder;

/**
 * An optimize work for ES2, using the Optimize API.
 * <p>
 * The Optimize API has been deprecated in 2.1 in favor of the ForceMerge API,
 * but it still works on every 2.x version and we want this work
 * to be compatible with ES2.0 too.
 *
 * @author Yoann Rodiere
 */
public class ES2OptimizeWork extends SimpleElasticsearchWork<Void> {

	protected ES2OptimizeWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements OptimizeWorkBuilder {
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
					ElasticsearchRequest.post();

			if ( !indexNames.isEmpty() ) {
				builder.multiValuedPathComponent( indexNames );
			}

			builder.pathComponent( Paths._OPTIMIZE );

			return builder.build();
		}

		@Override
		public ES2OptimizeWork build() {
			return new ES2OptimizeWork( this );
		}
	}
}