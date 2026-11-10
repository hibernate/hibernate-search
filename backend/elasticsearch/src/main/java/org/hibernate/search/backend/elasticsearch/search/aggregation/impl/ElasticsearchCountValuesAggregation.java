package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.spi.CountValuesAggregationBuilder;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchCountValuesAggregation extends AbstractElasticsearchNestableAggregation<Long> {

	private static final JsonAccessor<ObjectNode> COUNT_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "value_count" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<ObjectNode> COUNT_DISTINCT_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "cardinality" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<String> FIELD_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "field" ).unwrapSingletonArrays().asString();

	private final String absoluteFieldPath;
	private final JsonAccessor<ObjectNode> operation;

	private ElasticsearchCountValuesAggregation(Builder builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.operation = builder.operation;
	}

	public static SearchQueryElementFactory<CountValuesAggregationBuilder.TypeSelector,
			ElasticsearchSearchIndexScope<?>,
			ElasticsearchSearchIndexNodeContext> factory() {
		return new Factory();
	}

	private static class Factory
			implements SearchQueryElementFactory<CountValuesAggregationBuilder.TypeSelector,
					ElasticsearchSearchIndexScope<?>,
					ElasticsearchSearchIndexNodeContext> {

		@Override
		public CountValuesAggregationBuilder.TypeSelector create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexNodeContext node) {
			return new TypeSelector( scope, node );
		}

		@Override
		public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
			if ( !getClass().equals( other.getClass() ) ) {
				throw QueryLog.INSTANCE.differentImplementationClassForQueryElement( getClass(), other.getClass() );
			}
		}
	}

	private static final class TypeSelector implements CountValuesAggregationBuilder.TypeSelector {
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexNodeContext node;

		private TypeSelector(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexNodeContext node) {
			this.scope = scope;
			this.node = node; // doesn't matter in this case
		}

		@Override
		public CountValuesAggregationBuilder builder() {
			return new ElasticsearchCountValuesAggregation.Builder( scope, node.toValueField() );
		}
	}

	@Override
	protected final ObjectNode doRequest(AggregationRequestBuildingContextContext context) {
		ObjectNode outerObject = JsonNodeFactory.instance.objectNode();
		ObjectNode innerObject = JsonNodeFactory.instance.objectNode();

		operation.set( outerObject, innerObject );
		FIELD_PROPERTY_ACCESSOR.set( innerObject, absoluteFieldPath );
		return outerObject;
	}

	@Override
	protected Extractor<Long> extractor(AggregationKey<?> key, AggregationRequestBuildingContextContext context) {
		return new MetricLongExtractor( key, nestedPathHierarchy, filter );
	}

	private static class MetricLongExtractor extends AbstractExtractor<Long> {
		protected MetricLongExtractor(
				AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter
		) {
			super( key, nestedPathHierarchy, filter );
		}

		@Override
		protected Long doExtract(ObjectNode aggregationResult, AggregationExtractContext context) {
			JsonNode value = aggregationResult.get( "value" );
			return JsonElementTypes.LONG.fromElement( value );
		}
	}

	private static class Builder extends AbstractBuilder<Long>
			implements CountValuesAggregationBuilder {
		private JsonAccessor<ObjectNode> operation;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field) {
			super( scope, field );
			this.operation = COUNT_PROPERTY_ACCESSOR;
		}

		@Override
		public ElasticsearchCountValuesAggregation build() {
			return new ElasticsearchCountValuesAggregation( this );
		}

		@Override
		public void distinct(boolean distinct) {
			if ( distinct ) {
				operation = COUNT_DISTINCT_PROPERTY_ACCESSOR;
			}
			else {
				operation = COUNT_PROPERTY_ACCESSOR;
			}
		}
	}
}
