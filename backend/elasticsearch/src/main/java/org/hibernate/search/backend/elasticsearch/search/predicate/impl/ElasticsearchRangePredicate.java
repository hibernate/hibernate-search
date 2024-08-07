/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchRangePredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor RANGE_ACCESSOR = JsonAccessor.root().property( "range" ).asObject();

	private static final JsonAccessor<JsonElement> GT_ACCESSOR = JsonAccessor.root().property( "gt" );
	private static final JsonAccessor<JsonElement> GTE_ACCESSOR = JsonAccessor.root().property( "gte" );
	private static final JsonAccessor<JsonElement> LT_ACCESSOR = JsonAccessor.root().property( "lt" );
	private static final JsonAccessor<JsonElement> LTE_ACCESSOR = JsonAccessor.root().property( "lte" );

	private final Range<JsonElement> range;

	private ElasticsearchRangePredicate(Builder<?> builder) {
		super( builder );
		range = builder.range;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		JsonAccessor<JsonElement> accessor;
		Optional<JsonElement> lowerBoundValue = range.lowerBoundValue();
		if ( lowerBoundValue.isPresent() ) {
			accessor = RangeBoundInclusion.EXCLUDED.equals( range.lowerBoundInclusion() ) ? GT_ACCESSOR : GTE_ACCESSOR;
			accessor.set( innerObject, lowerBoundValue.get() );
		}
		Optional<JsonElement> upperBoundValue = range.upperBoundValue();
		if ( upperBoundValue.isPresent() ) {
			accessor = RangeBoundInclusion.EXCLUDED.equals( range.upperBoundInclusion() ) ? LT_ACCESSOR : LTE_ACCESSOR;
			accessor.set( innerObject, upperBoundValue.get() );
		}

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );
		RANGE_ACCESSOR.set( outerObject, middleObject );

		return outerObject;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<RangePredicateBuilder, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public RangePredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder implements RangePredicateBuilder {

		private final ElasticsearchSearchIndexValueFieldContext<F> field;
		private final ElasticsearchFieldCodec<F> codec;

		private Range<JsonElement> range;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
			this.field = field;
		}

		@Override
		public void within(Range<?> range, ValueModel lowerBoundModel, ValueModel upperBoundModel) {
			this.range = Range.between(
					convertToFieldValue( range.lowerBoundValue(), lowerBoundModel ),
					range.lowerBoundInclusion(),
					convertToFieldValue( range.upperBoundValue(), upperBoundModel ),
					range.upperBoundInclusion()
			);
		}

		@Override
		public SearchPredicate build() {
			// Check analyzer compatibility for multi-index search
			field.type().searchAnalyzerName();
			field.type().normalizerName();

			return new ElasticsearchRangePredicate( this );
		}

		private JsonElement convertToFieldValue(Optional<?> valueOptional, ValueModel valueModel) {
			if ( valueOptional.isEmpty() ) {
				return null;
			}
			Object value = valueOptional.get();
			return field.encodingContext().convertAndEncode( scope, field, value, valueModel, ElasticsearchFieldCodec::encode );
		}
	}
}
