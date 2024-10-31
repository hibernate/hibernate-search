/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchStandardMatchPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonAccessor<JsonElement> QUERY_ACCESSOR = JsonAccessor.root().property( "query" );

	private static final JsonObjectAccessor MATCH_ACCESSOR = JsonAccessor.root().property( "match" ).asObject();

	private final JsonElement value;

	ElasticsearchStandardMatchPredicate(Builder<?> builder) {
		super( builder );
		value = builder.value;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		QUERY_ACCESSOR.set( innerObject, value );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		MATCH_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<MatchPredicateBuilder, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public MatchPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	static class Builder<F> extends AbstractBuilder implements MatchPredicateBuilder {

		protected final ElasticsearchSearchIndexValueFieldContext<F> field;
		private final ElasticsearchFieldCodec<F> codec;

		private JsonElement value;

		Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.field = field;
			this.codec = codec;
		}

		@Override
		public void fuzzy(int maxEditDistance, int exactPrefixLength) {
			throw QueryLog.INSTANCE
					.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public void analyzer(String analyzerName) {
			throw QueryLog.INSTANCE
					.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public void skipAnalysis() {
			throw QueryLog.INSTANCE
					.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public void value(Object value, ValueModel valueModel) {
			this.value = field.encodingContext().convertAndEncode( scope, field, value, valueModel,
					ElasticsearchFieldCodec::encode );
		}

		@Override
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			throw QueryLog.INSTANCE
					.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			throw QueryLog.INSTANCE
					.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchStandardMatchPredicate( this );
		}
	}
}
