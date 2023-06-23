/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.engine.common.timing.Deadline;

import com.google.gson.JsonObject;

public class ScrollWork<R> extends AbstractNonBulkableWork<R> {

	private final ElasticsearchSearchResultExtractor<R> resultExtractor;
	private final Deadline deadline;
	private final boolean failOnDeadline;

	protected ScrollWork(Builder<R> builder) {
		super( builder );
		this.resultExtractor = builder.resultExtractor;
		this.deadline = builder.deadline;
		this.failOnDeadline = builder.failOnDeadline;
	}

	@Override
	protected R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.body();
		return resultExtractor.extract( body, failOnDeadline ? deadline : null );
	}

	public static class Builder<R>
			extends AbstractBuilder<Builder<R>> {
		private final String scrollId;
		private final String scrollTimeout;
		private final ElasticsearchSearchResultExtractor<R> resultExtractor;
		private Deadline deadline;
		private boolean failOnDeadline;

		public Builder(String scrollId, String scrollTimeout, ElasticsearchSearchResultExtractor<R> resultExtractor) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.scrollId = scrollId;
			this.scrollTimeout = scrollTimeout;
			this.resultExtractor = resultExtractor;
		}

		public Builder<R> deadline(Deadline deadline, boolean failOnDeadline) {
			this.deadline = deadline;
			this.failOnDeadline = failOnDeadline;
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			JsonObject body = new JsonObject();
			body.addProperty( "scroll_id", scrollId );
			body.addProperty( "scroll", scrollTimeout );

			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
							.pathComponent( Paths._SEARCH )
							.pathComponent( Paths.SCROLL )
							.body( body );

			return builder.build();
		}

		@Override
		public ScrollWork<R> build() {
			return new ScrollWork<>( this );
		}
	}
}
