/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AnalyzeWork extends AbstractNonBulkableWork<List<? extends AnalysisToken>> {
	private static final JsonArrayAccessor TOKENS_ACCESSOR = JsonAccessor.root().property( "tokens" ).asArray();
	private static final JsonAccessor<String> TOKEN_ACCESSOR = JsonAccessor.root().property( "token" ).asString();
	private static final JsonAccessor<Integer> START_OFFSET_ACCESSOR =
			JsonAccessor.root().property( "start_offset" ).asInteger();
	private static final JsonAccessor<Integer> END_OFFSET_ACCESSOR = JsonAccessor.root().property( "end_offset" ).asInteger();
	private static final JsonAccessor<String> TYPE_ACCESSOR = JsonAccessor.root().property( "type" ).asString();

	private static final ElasticsearchRequestSuccessAssessor SUCCESS_ASSESSOR =
			ElasticsearchRequestSuccessAssessor.builder().build();

	private AnalyzeWork(Builder builder) {
		super( builder );
	}

	@Override
	protected List<? extends AnalysisToken> generateResult(ElasticsearchWorkExecutionContext context,
			ElasticsearchResponse response) {
		JsonObject body = response.body();

		// we don't use Gson to keep ElasticsearchAnalysisToken immutable.
		List<ElasticsearchAnalysisToken> tokens = new ArrayList<>();
		for ( JsonElement element : TOKENS_ACCESSOR.getOrCreate( body, JsonArray::new ) ) {
			JsonObject token = element.getAsJsonObject();
			tokens.add( new ElasticsearchAnalysisToken(
					TOKEN_ACCESSOR.get( token )
							.orElseThrow( () -> this.missingRequiredPropertyInResponse( "token" ) ),
					START_OFFSET_ACCESSOR.get( token )
							.orElseThrow( () -> this.missingRequiredPropertyInResponse( "start_offset" ) ),
					END_OFFSET_ACCESSOR.get( token )
							.orElseThrow( () -> this.missingRequiredPropertyInResponse( "end_offset" ) ),
					TYPE_ACCESSOR.get( token )
							.orElseThrow( () -> this.missingRequiredPropertyInResponse( "type" ) )
			) );
		}

		return tokens;
	}

	private AssertionFailure missingRequiredPropertyInResponse(String property) {
		return new AssertionFailure( "The required property '" + property + "' is missing in the response." );
	}

	public static class Builder extends AbstractBuilder<Builder> {
		private final URLEncodedString indexName;
		private final JsonObject payload;

		public static Builder create(URLEncodedString indexName, String text, String analyzer, String normalizer) {
			if ( analyzer == null && normalizer == null ) {
				throw new AssertionFailure( "Either an analyzer or a normalizer should have been passed" );
			}

			JsonObject payload = new JsonObject();

			payload.addProperty( "text", text );
			if ( analyzer != null ) {
				payload.addProperty( "analyzer", analyzer );
			}
			if ( normalizer != null ) {
				payload.addProperty( "normalizer", normalizer );
			}

			return new Builder( indexName, payload );
		}

		private Builder(URLEncodedString indexName, JsonObject payload) {
			super( SUCCESS_ASSESSOR );
			this.indexName = indexName;
			this.payload = payload;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
							.pathComponent( indexName )
							.pathComponent( Paths._ANALYZE )
							.body( payload );

			return builder.build();
		}

		@Override
		public AnalyzeWork build() {
			return new AnalyzeWork( this );
		}
	}

	private static class ElasticsearchAnalysisToken implements AnalysisToken {

		private final String term;
		private final int startOffset;
		private final int endOffset;
		private final String type;

		private ElasticsearchAnalysisToken(String term, int startOffset, int endOffset, String type) {
			this.term = term;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.type = type;
		}

		@Override
		public String term() {
			return term;
		}

		@Override
		public int startOffset() {
			return startOffset;
		}

		@Override
		public int endOffset() {
			return endOffset;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String toString() {
			return "AnalysisToken{" +
					"term='" + term + '\'' +
					", startOffset=" + startOffset +
					", endOffset=" + endOffset +
					", type='" + type + '\'' +
					'}';
		}
	}
}
