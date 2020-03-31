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
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.AbstractElasticsearchSearchSortBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;

abstract class AbstractElasticsearchDocumentValueSortBuilder extends AbstractElasticsearchSearchSortBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// new API
	private static final JsonAccessor<JsonElement> NESTED_ACCESSOR = JsonAccessor.root().property( "nested" );
	private static final JsonAccessor<JsonElement> PATH_ACCESSOR = JsonAccessor.root().property( "path" );
	private static final JsonAccessor<JsonElement> FILTER_ACCESSOR = JsonAccessor.root().property( "filter" );
	// old API
	private static final JsonAccessor<JsonElement> NESTED_PATH_ACCESSOR = JsonAccessor.root().property( "nested_path" );

	private static final JsonAccessor<JsonElement> MODE_ACCESSOR = JsonAccessor.root().property( "mode" );
	private static final JsonPrimitive SUM_KEYWORD_JSON = new JsonPrimitive( "sum" );
	private static final JsonPrimitive AVG_KEYWORD_JSON = new JsonPrimitive( "avg" );
	private static final JsonPrimitive MIN_KEYWORD_JSON = new JsonPrimitive( "min" );
	private static final JsonPrimitive MAX_KEYWORD_JSON = new JsonPrimitive( "max" );
	private static final JsonPrimitive MEDIAN_KEYWORD_JSON = new JsonPrimitive( "median" );

	protected final String absoluteFieldPath;
	protected final List<String> nestedPathHierarchy;
	private final ElasticsearchSearchSyntax searchSyntax;

	private JsonPrimitive mode;

	AbstractElasticsearchDocumentValueSortBuilder(String absoluteFieldPath, List<String> nestedPathHierarchy,
			ElasticsearchSearchSyntax searchSyntax) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedPathHierarchy = nestedPathHierarchy;
		this.searchSyntax = searchSyntax;
	}

	public void mode(SortMode mode) {
		if ( !nestedPathHierarchy.isEmpty() && SortMode.MEDIAN.equals( mode ) ) {
			throw log.cannotComputeMedianAcrossNested( getEventContext() );
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

	@Override
	protected void enrichInnerObject(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( !nestedPathHierarchy.isEmpty() ) {
			if ( searchSyntax.useOldSortNestedApi() ) {
				// the old api requires only the last path ( the deepest one )
				String lastNestedPath = nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

				NESTED_PATH_ACCESSOR.set( innerObject, new JsonPrimitive( lastNestedPath ) );
			}
			else {
				JsonObject jsonFilter = null;
				if ( filter instanceof ElasticsearchSearchPredicateBuilder ) {
					ElasticsearchSearchPredicateContext filterContext = collector.getRootPredicateContext();
					jsonFilter = ((ElasticsearchSearchPredicateBuilder) filter).build( filterContext );
				}

				JsonObject nextNestedObjectTarget = innerObject;
				for ( String nestedPath : nestedPathHierarchy ) {
					JsonObject nestedObject = new JsonObject();
					PATH_ACCESSOR.set( nestedObject, new JsonPrimitive( nestedPath ) );
					NESTED_ACCESSOR.set( nextNestedObjectTarget, nestedObject );
					if ( jsonFilter != null ) {
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

	protected final EventContext getEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath );
	}
}
