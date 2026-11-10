package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.engine.common.timing.Deadline;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

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
	protected R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		ObjectNode body = response.bodyJson();
		return resultExtractor.extract( body, failOnDeadline ? deadline : null );
	}

	public static class Builder<R> extends AbstractBuilder<Builder<R>> {

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
		protected ElasticsearchJsonRequest buildRequest() {
			ObjectNode body = new ObjectNode( JsonNodeFactory.instance );
			body.put( "scroll_id", scrollId );
			body.put( "scroll", scrollTimeout );
			ElasticsearchJsonRequest.Builder builder =
					ElasticsearchJsonRequest.post().pathComponent( Paths._SEARCH ).pathComponent( Paths.SCROLL )
							.bodyJsonFromInternal( body );
			return builder.build();
		}

		@Override
		public ScrollWork<R> build() {
			return new ScrollWork<>( this );
		}
	}
}
