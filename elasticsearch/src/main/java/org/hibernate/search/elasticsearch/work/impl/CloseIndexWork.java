/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.work.impl.builder.CloseIndexWorkBuilder;

public class CloseIndexWork extends SimpleElasticsearchWork<Void> {

	protected CloseIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements CloseIndexWorkBuilder {

		private final URLEncodedString indexName;

		public Builder(URLEncodedString indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.pathComponent( indexName )
					.pathComponent( Paths._CLOSE );

			return builder.build();
		}

		@Override
		public CloseIndexWork build() {
			return new CloseIndexWork( this );
		}
	}
}