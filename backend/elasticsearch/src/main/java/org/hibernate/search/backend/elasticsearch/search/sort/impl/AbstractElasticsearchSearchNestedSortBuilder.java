/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class AbstractElasticsearchSearchNestedSortBuilder extends AbstractElasticsearchSearchSortBuilder {

	private static final JsonAccessor<JsonElement> NESTED = JsonAccessor.root().property( "nested" );
	private static final JsonAccessor<JsonElement> PATH = JsonAccessor.root().property( "path" );

	private final List<String> nestedPathHierarchy;

	public AbstractElasticsearchSearchNestedSortBuilder(List<String> nestedPathHierarchy) {
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	@Override
	protected void enrichInnerObject(JsonObject innerObject) {
		JsonObject nextNestedObjectTarget = innerObject;
		for ( String nestedPath : nestedPathHierarchy ) {
			JsonObject nestedObject = new JsonObject();
			PATH.set( nestedObject, new JsonPrimitive( nestedPath ) );
			NESTED.set( nextNestedObjectTarget, nestedObject );
			nextNestedObjectTarget = nestedObject;
		}
	}
}
