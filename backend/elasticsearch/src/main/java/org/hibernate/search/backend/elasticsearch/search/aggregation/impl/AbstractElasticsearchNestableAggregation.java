package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateNestingContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public abstract class AbstractElasticsearchNestableAggregation<A> extends AbstractElasticsearchAggregation<A> {

	private static final JsonAccessor<String> REQUEST_NESTED_PATH_ACCESSOR =
			JsonAccessor.root().property( "nested" ).unwrapSingletonArrays().property( "path" ).unwrapSingletonArrays()
					.asString();
	private static final JsonObjectAccessor REQUEST_FILTER_ACCESSOR =
			JsonAccessor.root().property( "filter" ).unwrapSingletonArrays().asObject();

	private static final String NESTED_NAME = "nested";
	private static final JsonAccessor<ObjectNode> REQUEST_AGGREGATIONS_NESTED_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).unwrapSingletonArrays().property( NESTED_NAME )
					.unwrapSingletonArrays().asObject();
	private static final JsonAccessor<ObjectNode> RESPONSE_NESTED_ACCESSOR =
			JsonAccessor.root().property( NESTED_NAME ).unwrapSingletonArrays().asObject();

	private static final String FILTERED_NAME = "filtered";
	private static final JsonAccessor<ObjectNode> REQUEST_AGGREGATIONS_FILTERED_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).unwrapSingletonArrays().property( FILTERED_NAME )
					.unwrapSingletonArrays().asObject();
	private static final JsonAccessor<ObjectNode> RESPONSE_FILTERED_ACCESSOR =
			JsonAccessor.root().property( FILTERED_NAME ).unwrapSingletonArrays().asObject();

	protected final List<String> nestedPathHierarchy;
	protected final ElasticsearchSearchPredicate filter;

	AbstractElasticsearchNestableAggregation(AbstractBuilder<A> builder) {
		super( builder );
		nestedPathHierarchy = builder.nestedPathHierarchy;
		filter = builder.filter;
	}

	@Override
	public final Extractor<A> request(AggregationRequestContext context, AggregationKey<?> key, ObjectNode jsonAggregations) {
		AggregationRequestBuildingContextContext buildingContext = new AggregationRequestBuildingContextContext( context );
		jsonAggregations.set( key.name(), request( buildingContext ) );
		return extractor( key, buildingContext );
	}

	private ObjectNode request(AggregationRequestBuildingContextContext context) {
		ObjectNode result = doRequest( context );

		if ( nestedPathHierarchy.isEmpty() ) {
			// Implicit nesting is not necessary
			return result;
		}

		if ( filter != null ) {
			PredicateRequestContext filterContext = context.getRootPredicateContext()
					.withNestedPath( nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 ) );
			ObjectNode jsonFilter = filter.toJsonQuery( filterContext );

			ObjectNode object = JsonNodeFactory.instance.objectNode();

			REQUEST_FILTER_ACCESSOR.set( object, jsonFilter );
			REQUEST_AGGREGATIONS_FILTERED_ACCESSOR.set( object, result );
			result = object;
		}

		// traversing the nestedPathHierarchy in reverse order
		int hierarchyLastIndex = nestedPathHierarchy.size() - 1;
		for ( int i = hierarchyLastIndex; i >= 0; i-- ) {
			String path = nestedPathHierarchy.get( i );

			ObjectNode object = JsonNodeFactory.instance.objectNode();

			REQUEST_NESTED_PATH_ACCESSOR.set( object, path );
			REQUEST_AGGREGATIONS_NESTED_ACCESSOR.set( object, result );

			result = object;
		}

		return result;
	}

	protected abstract ObjectNode doRequest(AggregationRequestBuildingContextContext context);

	protected abstract Extractor<A> extractor(AggregationKey<?> key, AggregationRequestBuildingContextContext context);

	protected abstract static class AbstractExtractor<T> implements Extractor<T> {

		private final AggregationKey<?> key;
		private final List<String> nestedPathHierarchy;
		private final ElasticsearchSearchPredicate filter;

		protected AbstractExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter) {
			this.key = key;
			this.nestedPathHierarchy = nestedPathHierarchy;
			this.filter = filter;
		}

		@Override
		public final T extract(ObjectNode aggregationResult, AggregationExtractContext context) {
			int nestedPathHierarchySize = nestedPathHierarchy.size();

			ObjectNode actualAggregationResult = ( (ObjectNode) aggregationResult.get( key.name() ) );

			for ( int i = 0; i < nestedPathHierarchySize; ++i ) {
				actualAggregationResult = RESPONSE_NESTED_ACCESSOR.get( actualAggregationResult )
						.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
			}

			if ( filter != null ) {
				actualAggregationResult = RESPONSE_FILTERED_ACCESSOR.get( actualAggregationResult )
						.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
			}

			return doExtract( actualAggregationResult, context );
		}

		protected abstract T doExtract(ObjectNode aggregationResult, AggregationExtractContext context);

		@Override
		public AggregationKey<?> key() {
			return key;
		}
	}


	protected final boolean isNested() {
		return !nestedPathHierarchy.isEmpty();
	}

	public abstract static class AbstractBuilder<A> extends AbstractElasticsearchAggregation.AbstractBuilder<A> {

		protected final ElasticsearchSearchIndexValueFieldContext<?> field;
		protected final List<String> nestedPathHierarchy;
		private ElasticsearchSearchPredicate filter;

		public AbstractBuilder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field) {
			super( scope );
			this.field = field;
			this.nestedPathHierarchy = field.nestedPathHierarchy();
		}

		public void filter(SearchPredicate filter) {
			if ( nestedPathHierarchy.isEmpty() ) {
				throw QueryLog.INSTANCE.cannotFilterAggregationOnRootDocumentField( field.absolutePath(),
						field.eventContext() );
			}
			ElasticsearchSearchPredicate elasticsearchFilter = ElasticsearchSearchPredicate.from( scope, filter );
			elasticsearchFilter.checkNestableWithin(
					PredicateNestingContext.nested( nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 ) ) );
			this.filter = elasticsearchFilter;
		}

		@Override
		public abstract ElasticsearchSearchAggregation<A> build();
	}
}
