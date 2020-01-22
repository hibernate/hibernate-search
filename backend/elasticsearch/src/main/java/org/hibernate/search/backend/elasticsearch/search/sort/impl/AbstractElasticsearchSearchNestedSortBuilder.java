/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchJsonSyntaxHelper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class AbstractElasticsearchSearchNestedSortBuilder extends AbstractElasticsearchSearchSortBuilder {

	// new API
	private static final JsonAccessor<JsonElement> NESTED = JsonAccessor.root().property( "nested" );
	private static final JsonAccessor<JsonElement> PATH = JsonAccessor.root().property( "path" );

	// old API
	private static final JsonAccessor<JsonElement> NESTED_PATH = JsonAccessor.root().property( "nested_path" );

	private final List<String> nestedPathHierarchy;
	private final ElasticsearchJsonSyntaxHelper jsonSyntaxHelper;

	public AbstractElasticsearchSearchNestedSortBuilder(List<String> nestedPathHierarchy, ElasticsearchJsonSyntaxHelper jsonSyntaxHelper) {
		this.nestedPathHierarchy = nestedPathHierarchy;
		this.jsonSyntaxHelper = jsonSyntaxHelper;
	}

	@Override
	protected void enrichInnerObject(JsonObject innerObject) {
		if ( nestedPathHierarchy.isEmpty() ) {
			return;
		}
		if ( jsonSyntaxHelper.useOldSortNestedApi() ) {
			// the old api requires only the last path ( the deepest one )
			String lastNestedPath = nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

			NESTED_PATH.set( innerObject, new JsonPrimitive( lastNestedPath ) );
			return;
		}

		JsonObject nextNestedObjectTarget = innerObject;
		for ( String nestedPath : nestedPathHierarchy ) {
			JsonObject nestedObject = new JsonObject();
			PATH.set( nestedObject, new JsonPrimitive( nestedPath ) );
			NESTED.set( nextNestedObjectTarget, nestedObject );

			// the new api requires a recursion on the path hierarchy
			nextNestedObjectTarget = nestedObject;
		}
	}
}
