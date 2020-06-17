/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.AbstractElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

abstract class AbstractElasticsearchDocumentValueSortBuilder<F> extends AbstractElasticsearchSearchSortBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// new API
	private static final JsonAccessor<JsonElement> NESTED_ACCESSOR = JsonAccessor.root().property( "nested" );
	private static final JsonAccessor<JsonElement> PATH_ACCESSOR = JsonAccessor.root().property( "path" );
	private static final JsonAccessor<JsonElement> FILTER_ACCESSOR = JsonAccessor.root().property( "filter" );
	// old API
	private static final JsonAccessor<JsonElement> NESTED_PATH_ACCESSOR = JsonAccessor.root().property( "nested_path" );
	private static final JsonAccessor<JsonElement> NESTED_FILTER_ACCESSOR = JsonAccessor.root().property( "nested_filter" );

	private static final JsonAccessor<JsonElement> MODE_ACCESSOR = JsonAccessor.root().property( "mode" );
	private static final JsonPrimitive SUM_KEYWORD_JSON = new JsonPrimitive( "sum" );
	private static final JsonPrimitive AVG_KEYWORD_JSON = new JsonPrimitive( "avg" );
	private static final JsonPrimitive MIN_KEYWORD_JSON = new JsonPrimitive( "min" );
	private static final JsonPrimitive MAX_KEYWORD_JSON = new JsonPrimitive( "max" );
	private static final JsonPrimitive MEDIAN_KEYWORD_JSON = new JsonPrimitive( "median" );

	private final ElasticsearchSearchContext searchContext;
	protected final ElasticsearchSearchFieldContext<F> field;
	protected final List<String> nestedPathHierarchy;

	private JsonPrimitive mode;
	private ElasticsearchSearchPredicate filter;

	AbstractElasticsearchDocumentValueSortBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field) {
		this.searchContext = searchContext;
		this.field = field;
		this.nestedPathHierarchy = field.nestedPathHierarchy();
	}

	public void mode(SortMode mode) {
		if ( !nestedPathHierarchy.isEmpty() && SortMode.MEDIAN.equals( mode ) ) {
			throw log.cannotComputeMedianAcrossNested( field.eventContext() );
		}
		if ( mode != null ) {
			switch ( mode ) {
				case SUM:
					this.mode = SUM_KEYWORD_JSON;
					break;
				case AVG:
					this.mode = AVG_KEYWORD_JSON;
					break;
				case MIN:
					this.mode = MIN_KEYWORD_JSON;
					break;
				case MAX:
					this.mode = MAX_KEYWORD_JSON;
					break;
				case MEDIAN:
					this.mode = MEDIAN_KEYWORD_JSON;
					break;
				default:
					throw new AssertionFailure( "Unexpected sort mode: " + mode );
			}
		}
	}

	public void filter(SearchPredicate filter) {
		if ( nestedPathHierarchy.isEmpty() ) {
			throw log.cannotFilterSortOnRootDocumentField( field.absolutePath(), field.eventContext() );
		}
		ElasticsearchSearchPredicate elasticsearchFilter = ElasticsearchSearchPredicate.from( searchContext, filter );
		elasticsearchFilter.checkNestableWithin( nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 ) );
		this.filter = elasticsearchFilter;
	}

	@Override
	protected void enrichInnerObject(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( !nestedPathHierarchy.isEmpty() ) {
			if ( searchContext.searchSyntax().useOldSortNestedApi() ) {
				// the old api requires only the last path ( the deepest one )
				String lastNestedPath = nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

				NESTED_PATH_ACCESSOR.set( innerObject, new JsonPrimitive( lastNestedPath ) );
				if ( filter != null ) {
					PredicateRequestContext filterContext = collector.getRootPredicateContext()
							.withNestedPath( lastNestedPath );
					JsonObject jsonFilter = getJsonFilter( filterContext );
					NESTED_FILTER_ACCESSOR.set( innerObject, jsonFilter );
				}
			}
			else {
				JsonObject nextNestedObjectTarget = innerObject;
				for ( int i = 0; i < nestedPathHierarchy.size(); i++ ) {
					String nestedPath = nestedPathHierarchy.get( i );

					JsonObject nestedObject = new JsonObject();
					PATH_ACCESSOR.set( nestedObject, new JsonPrimitive( nestedPath ) );
					NESTED_ACCESSOR.set( nextNestedObjectTarget, nestedObject );
					if ( i == (nestedPathHierarchy.size() - 1) && filter != null ) {
						PredicateRequestContext filterContext = collector.getRootPredicateContext()
								.withNestedPath( nestedPath );
						JsonObject jsonFilter = getJsonFilter( filterContext );
						FILTER_ACCESSOR.set( nestedObject, jsonFilter );
					}

					// the new api requires a recursion on the path hierarchy
					nextNestedObjectTarget = nestedObject;
				}
			}
		}

		if ( mode != null ) {
			MODE_ACCESSOR.set( innerObject, mode );
		}
	}

	private JsonObject getJsonFilter(PredicateRequestContext filterContext) {
		return filter.toJsonQuery( filterContext );
	}
}
