package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.engine.search.aggregation.AggregationKey;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractElasticsearchBucketAggregation<K, V>
		extends AbstractElasticsearchNestableAggregation<Map<K, V>> {

	protected static final JsonAccessor<ObjectNode> REQUEST_AGGREGATIONS_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<ObjectNode> REQUEST_REVERSE_NESTED_ACCESSOR =
			JsonAccessor.root().property( "reverse_nested" ).unwrapSingletonArrays().asObject();

	protected static final JsonAccessor<ObjectNode> REQUEST_REVERSE_NESTED_WRAPPER_ACCESSOR =
			JsonAccessor.root().property( "reverse_nested_wrapper" ).unwrapSingletonArrays().asObject();

	private static final String ROOT_DOC_COUNT_NAME = "root_doc_count";
	private static final JsonAccessor<ObjectNode> REQUEST_AGGREGATIONS_ROOT_DOC_COUNT_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).unwrapSingletonArrays().property( ROOT_DOC_COUNT_NAME )
					.unwrapSingletonArrays().asObject();

	protected static final String INNER_EXTRACTOR = "innerExtractor";

	AbstractElasticsearchBucketAggregation(AbstractBuilder<K, V> builder) {
		super( builder );
	}

	@Override
	protected final ObjectNode doRequest(AggregationRequestBuildingContextContext context) {
		ObjectNode outerObject = JsonNodeFactory.instance.objectNode();
		ObjectNode innerObject = JsonNodeFactory.instance.objectNode();

		doRequest( outerObject, innerObject, context );

		if ( isNested() ) {
			// if we are looking at a nested composite/non-document-count sub-aggregation
			//  we have to wrap it in a single aggregation with `reverse_nested` in it.
			if ( !innerObject.isEmpty() ) {
				ObjectNode aggregations = JsonNodeFactory.instance.objectNode();
				ObjectNode wrapper = JsonNodeFactory.instance.objectNode();
				REQUEST_REVERSE_NESTED_WRAPPER_ACCESSOR.set( aggregations, wrapper );
				REQUEST_REVERSE_NESTED_ACCESSOR.set( wrapper, JsonNodeFactory.instance.objectNode() );
				REQUEST_AGGREGATIONS_ACCESSOR.set( wrapper, innerObject );
				REQUEST_AGGREGATIONS_ACCESSOR.set( outerObject, aggregations );
			}
			else {
				// we are looking at the "default" "just give me the counts" aggregation here:
				ObjectNode rootDocCountSubAggregationOuterObject = JsonNodeFactory.instance.objectNode();
				ObjectNode rootDocCountSubAggregationInnerObject = JsonNodeFactory.instance.objectNode();

				REQUEST_REVERSE_NESTED_ACCESSOR.set(
						rootDocCountSubAggregationOuterObject,
						rootDocCountSubAggregationInnerObject
				);
				REQUEST_AGGREGATIONS_ROOT_DOC_COUNT_ACCESSOR.set( outerObject, rootDocCountSubAggregationOuterObject );
			}
		}
		else if ( !innerObject.isEmpty() ) {
			// regular bucket/composite aggregation requested:
			REQUEST_AGGREGATIONS_ACCESSOR.set( outerObject, innerObject );
		}

		return outerObject;
	}

	protected abstract void doRequest(ObjectNode outerObject, ObjectNode innerObject,
			AggregationRequestBuildingContextContext context);

	protected abstract class AbstractBucketExtractor<A, B> extends AbstractExtractor<Map<A, B>> {

		protected AbstractBucketExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter) {
			super( key, nestedPathHierarchy, filter );
		}

		@Override
		protected final Map<A, B> doExtract(ObjectNode aggregationResult, AggregationExtractContext context) {
			JsonNode buckets = aggregationResult.get( "buckets" );

			return doExtract( context, buckets );
		}

		protected abstract Map<A, B> doExtract(AggregationExtractContext context, JsonNode buckets);
	}

	public abstract static class AbstractBuilder<K, V>
			extends AbstractElasticsearchNestableAggregation.AbstractBuilder<Map<K, V>> {

		public AbstractBuilder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<?> field) {
			super( scope, field );
		}

		@Override
		public abstract ElasticsearchSearchAggregation<Map<K, V>> build();
	}
}
