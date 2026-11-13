package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchRangePredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor RANGE_ACCESSOR =
			JsonAccessor.root().property( "range" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<JsonNode> GT_ACCESSOR = JsonAccessor.root().property( "gt" );
	private static final JsonAccessor<JsonNode> GTE_ACCESSOR = JsonAccessor.root().property( "gte" );
	private static final JsonAccessor<JsonNode> LT_ACCESSOR = JsonAccessor.root().property( "lt" );
	private static final JsonAccessor<JsonNode> LTE_ACCESSOR = JsonAccessor.root().property( "lte" );

	private final Range<JsonNode> range;

	private ElasticsearchRangePredicate(Builder<?> builder) {
		super( builder );
		range = builder.range;
	}

	@Override
	protected ObjectNode doToJsonQuery(PredicateRequestContext context, ObjectNode outerObject,
			ObjectNode innerObject) {
		JsonAccessor<JsonNode> accessor;
		Optional<JsonNode> lowerBoundValue = range.lowerBoundValue();
		if ( lowerBoundValue.isPresent() ) {
			accessor = RangeBoundInclusion.EXCLUDED.equals( range.lowerBoundInclusion() ) ? GT_ACCESSOR : GTE_ACCESSOR;
			accessor.set( innerObject, lowerBoundValue.get() );
		}
		Optional<JsonNode> upperBoundValue = range.upperBoundValue();
		if ( upperBoundValue.isPresent() ) {
			accessor = RangeBoundInclusion.EXCLUDED.equals( range.upperBoundInclusion() ) ? LT_ACCESSOR : LTE_ACCESSOR;
			accessor.set( innerObject, upperBoundValue.get() );
		}

		ObjectNode middleObject = JsonNodeFactory.instance.objectNode();
		middleObject.set( absoluteFieldPath, innerObject );
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

		private Range<JsonNode> range;

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

		private JsonNode convertToFieldValue(Optional<?> valueOptional, ValueModel valueModel) {
			if ( valueOptional.isEmpty() ) {
				return null;
			}
			Object value = valueOptional.get();
			return field.encodingContext().convertAndEncode( scope, field, value, valueModel, ElasticsearchFieldCodec::encode );
		}
	}
}
