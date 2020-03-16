/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonObject;

public abstract class AbstractElasticsearchSearchNestedPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder {

	private static final JsonAccessor<String> PATH_ACCESSOR = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asObject();

	private final List<String> nestedPathHierarchy;

	public AbstractElasticsearchSearchNestedPredicateBuilder(List<String> nestedPathHierarchy) {
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	@Override
	public final JsonObject build(ElasticsearchSearchPredicateContext context) {
		JsonObject result = super.build( context );
		if ( nestedPathHierarchy != null && !nestedPathHierarchy.isEmpty() ) {
			result = applyImplicitNested( result, nestedPathHierarchy, context );
		}
		return result;
	}

	public static JsonObject applyImplicitNested(JsonObject partialResult, List<String> nestedPathHierarchy, ElasticsearchSearchPredicateContext context) {
		JsonObject result = partialResult;

		// traversing the nestedPathHierarchy in the inverted order
		for ( int i = 0; i < nestedPathHierarchy.size(); i++ ) {
			String path = nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 - i );
			if ( path.equals( context.getNestedPath() ) ) {
				// skip all from this point
				break;
			}

			JsonObject innerObject = new JsonObject();

			PATH_ACCESSOR.set( innerObject, path );
			QUERY_ACCESSOR.set( innerObject, result );

			JsonObject outerObject = new JsonObject();
			outerObject.add( "nested", innerObject );
			result = outerObject;
		}

		return result;
	}
}
