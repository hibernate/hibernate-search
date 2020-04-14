/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonObject;

public abstract class AbstractElasticsearchNestableAggregation<A> extends AbstractElasticsearchAggregation<A> {

	private static final JsonAccessor<String> REQUEST_NESTED_PATH_ACCESSOR =
			JsonAccessor.root().property( "nested" ).property( "path" ).asString();
	private static final String NESTED_NAME = "nested";
	private static final JsonAccessor<JsonObject> REQUEST_AGGREGATIONS_NESTED_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).property( NESTED_NAME ).asObject();
	private static final JsonAccessor<JsonObject> RESPONSE_NESTED_ACCESSOR =
			JsonAccessor.root().property( NESTED_NAME ).asObject();

	private final List<String> nestedPathHierarchy;

	AbstractElasticsearchNestableAggregation(AbstractBuilder<A> builder) {
		super( builder );
		nestedPathHierarchy = builder.getNestedPathHierarchy();
	}

	@Override
	public final JsonObject request(AggregationRequestContext context) {
		JsonObject result = doRequest( context );

		if ( nestedPathHierarchy.isEmpty() ) {
			// Implicit nesting is not necessary
			return result;
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
					.orElseThrow( () -> new AssertionFailure( "Missing sub-aggregation in JSON response" ) );
		}

		return doExtract( actualAggregationResult, context );
	}

	protected abstract A doExtract(JsonObject aggregationResult, AggregationExtractContext context);

	public abstract static class AbstractBuilder<A> extends AbstractElasticsearchAggregation.AbstractBuilder<A> {

		public AbstractBuilder(ElasticsearchSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public abstract ElasticsearchSearchAggregation<A> build();

		protected abstract List<String> getNestedPathHierarchy();
	}
}
