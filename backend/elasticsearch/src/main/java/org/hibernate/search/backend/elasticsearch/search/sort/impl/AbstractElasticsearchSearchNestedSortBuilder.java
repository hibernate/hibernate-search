/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;

public abstract class AbstractElasticsearchSearchNestedSortBuilder extends AbstractElasticsearchSearchSortBuilder {

	// new API
	private static final JsonAccessor<JsonElement> NESTED_ACCESSOR = JsonAccessor.root().property( "nested" );
	private static final JsonAccessor<JsonElement> PATH_ACCESSOR = JsonAccessor.root().property( "path" );
	private static final JsonAccessor<JsonElement> FILTER_ACCESSOR = JsonAccessor.root().property( "filter" );

	// old API
	private static final JsonAccessor<JsonElement> NESTED_PATH_ACCESSOR = JsonAccessor.root().property( "nested_path" );

	private final List<String> nestedPathHierarchy;
	private final ElasticsearchSearchSyntax searchSyntax;

	public AbstractElasticsearchSearchNestedSortBuilder(List<String> nestedPathHierarchy, ElasticsearchSearchSyntax searchSyntax) {
		this.nestedPathHierarchy = nestedPathHierarchy;
		this.searchSyntax = searchSyntax;
	}

	@Override
	protected void enrichInnerObject(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( nestedPathHierarchy.isEmpty() ) {
			return;
		}
		if ( searchSyntax.useOldSortNestedApi() ) {
			// the old api requires only the last path ( the deepest one )
			String lastNestedPath = nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

			NESTED_PATH_ACCESSOR.set( innerObject, new JsonPrimitive( lastNestedPath ) );
			return;
		}

		JsonObject nextNestedObjectTarget = innerObject;
		for ( String nestedPath : nestedPathHierarchy ) {
			JsonObject nestedObject = new JsonObject();
			PATH_ACCESSOR.set( nestedObject, new JsonPrimitive( nestedPath ) );

			JsonObject jsonFilter = null;
			if ( filter instanceof ElasticsearchSearchPredicateBuilder ) {
				ElasticsearchSearchPredicateContext filterContext = collector.getRootPredicateContext();
				jsonFilter = ((ElasticsearchSearchPredicateBuilder) filter).build( filterContext );
			}

			if ( jsonFilter != null ) {
				FILTER_ACCESSOR.set( nestedObject, jsonFilter );
			}
			NESTED_ACCESSOR.set( nextNestedObjectTarget, nestedObject );

			// the new api requires a recursion on the path hierarchy
			nextNestedObjectTarget = nestedObject;
		}
	}
}
