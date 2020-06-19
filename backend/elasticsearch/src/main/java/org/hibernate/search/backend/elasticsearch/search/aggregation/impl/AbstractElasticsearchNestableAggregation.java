/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public abstract class AbstractElasticsearchNestableAggregation<A> extends AbstractElasticsearchAggregation<A> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> REQUEST_NESTED_PATH_ACCESSOR =
			JsonAccessor.root().property( "nested" ).property( "path" ).asString();
	private static final JsonObjectAccessor REQUEST_FILTER_ACCESSOR =
			JsonAccessor.root().property( "filter" ).asObject();

	private static final String NESTED_NAME = "nested";
	private static final JsonAccessor<JsonObject> REQUEST_AGGREGATIONS_NESTED_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).property( NESTED_NAME ).asObject();
	private static final JsonAccessor<JsonObject> RESPONSE_NESTED_ACCESSOR =
			JsonAccessor.root().property( NESTED_NAME ).asObject();

	private static final String FILTERED_NAME = "filtered";
	private static final JsonAccessor<JsonObject> REQUEST_AGGREGATIONS_FILTERED_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).property( FILTERED_NAME ).asObject();
	private static final JsonAccessor<JsonObject> RESPONSE_FILTERED_ACCESSOR =
			JsonAccessor.root().property( FILTERED_NAME ).asObject();

	private final List<String> nestedPathHierarchy;
	private final ElasticsearchSearchPredicate filter;

	AbstractElasticsearchNestableAggregation(AbstractBuilder<A> builder) {
		super( builder );
		nestedPathHierarchy = builder.nestedPathHierarchy;
		filter = builder.filter;
	}

	@Override
	public final JsonObject request(AggregationRequestContext context) {
		JsonObject result = doRequest( context );

		if ( nestedPathHierarchy.isEmpty() ) {
			// Implicit nesting is not necessary
			return result;
		}

		if ( filter != null ) {
			PredicateRequestContext filterContext = context.getRootPredicateContext()
					.withNestedPath( nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 ) );
			JsonObject jsonFilter = filter.toJsonQuery( filterContext );

			JsonObject object = new JsonObject();

			REQUEST_FILTER_ACCESSOR.set( object, jsonFilter );
			REQUEST_AGGREGATIONS_FILTERED_ACCESSOR.set( object, result );
			result = object;
		}

		// traversing the nestedPathHierarchy in reverse order
		int hierarchyLastIndex = nestedPathHierarchy.size() - 1;
		for ( int i = hierarchyLastIndex; i >= 0; i-- ) {
			String path = nestedPathHierarchy.get( i );

			JsonObject object = new JsonObject();

			REQUEST_NESTED_PATH_ACCESSOR.set( object, path );
			REQUEST_AGGREGATIONS_NESTED_ACCESSOR.set( object, result );

			result = object;
		}

		return result;
	}

	protected abstract JsonObject doRequest(AggregationRequestContext context);

	@Override
	public final A extract(JsonObject aggregationResult, AggregationExtractContext context) {
		int nestedPathHierarchySize = nestedPathHierarchy.size();

		JsonObject actualAggregationResult = aggregationResult;

		for ( int i = 0; i < nestedPathHierarchySize; ++i ) {
			actualAggregationResult = RESPONSE_NESTED_ACCESSOR.get( actualAggregationResult )
					.orElseThrow( log::elasticsearchResponseMissingData );
		}

		if ( filter != null ) {
			actualAggregationResult = RESPONSE_FILTERED_ACCESSOR.get( actualAggregationResult )
					.orElseThrow( log::elasticsearchResponseMissingData );
		}

		return doExtract( actualAggregationResult, context );
	}

	protected abstract A doExtract(JsonObject aggregationResult, AggregationExtractContext context);

	protected final boolean isNested() {
		return !nestedPathHierarchy.isEmpty();
	}

	public abstract static class AbstractBuilder<A> extends AbstractElasticsearchAggregation.AbstractBuilder<A> {

		protected final ElasticsearchSearchFieldContext<?> field;
		protected final List<String> nestedPathHierarchy;
		private ElasticsearchSearchPredicate filter;

		public AbstractBuilder(ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<?> field) {
			super( searchContext );
			this.field = field;
			this.nestedPathHierarchy = field.nestedPathHierarchy();
		}

		public void filter(SearchPredicate filter) {
			if ( nestedPathHierarchy.isEmpty() ) {
				throw log.cannotFilterAggregationOnRootDocumentField( field.absolutePath(), field.eventContext() );
			}
			ElasticsearchSearchPredicate elasticsearchFilter = ElasticsearchSearchPredicate.from( searchContext, filter );
			elasticsearchFilter.checkNestableWithin( nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 ) );
			this.filter = elasticsearchFilter;
		}

		@Override
		public abstract ElasticsearchSearchAggregation<A> build();
	}
}
