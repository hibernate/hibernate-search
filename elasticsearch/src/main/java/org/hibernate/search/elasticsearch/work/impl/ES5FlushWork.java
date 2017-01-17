/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.work.impl.builder.FlushWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.RefreshWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;

import com.google.gson.JsonObject;

/**
 * A flush work for ES5, using the Flush API then the Refresh API.
 * <p>
 * This is necessary because the "refresh" parameter in the Flush API has been removed
 * in ES5 (elasticsearch/elasticsearch:7cc48c8e8723d3b31fbcb371070bc2a8d87b1f7e).
 *
 * @author Yoann Rodiere
 */
public class ES5FlushWork extends SimpleElasticsearchWork<Void> {

	private final ElasticsearchWork<?> refreshWork;

	protected ES5FlushWork(Builder builder) {
		super( builder );
		this.refreshWork = builder.buildRefreshWork();
	}

	@Override
	protected void afterSuccess(ElasticsearchWorkExecutionContext executionContext) {
		super.afterSuccess( executionContext );
		refreshWork.execute( executionContext );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements FlushWorkBuilder {
		private final RefreshWorkBuilder refreshWorkBuilder;

		private List<String> indexNames = new ArrayList<>();

		public Builder(ElasticsearchWorkFactory workFactory) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.refreshWorkBuilder = workFactory.refresh();
		}

		@Override
		public Builder index(String indexName) {
			this.indexNames.add( indexName );
			this.refreshWorkBuilder.index( indexName );
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post();

			if ( !indexNames.isEmpty() ) {
				builder.multiValuedPathComponent( indexNames );
			}

			builder.pathComponent( "_flush" );

			return builder.build();
		}

		protected ElasticsearchWork<?> buildRefreshWork() {
			return refreshWorkBuilder.build();
		}

		@Override
		public ES5FlushWork build() {
			return new ES5FlushWork( this );
		}
	}
}