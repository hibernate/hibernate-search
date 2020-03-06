/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSearchNestedPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchTextWildcardPredicateBuilder extends AbstractElasticsearchSearchNestedPredicateBuilder
		implements WildcardPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor WILDCARD_ACCESSOR = JsonAccessor.root().property( "wildcard" ).asObject();

	private static final JsonAccessor<JsonElement> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );

	private final String absoluteFieldPath;

	private JsonElement pattern;

	public ElasticsearchTextWildcardPredicateBuilder(String absoluteFieldPath, List<String> nestedPathHierarchy) {
		super( nestedPathHierarchy );
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public void pattern(String pattern) {
		this.pattern = new JsonPrimitive( pattern );
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		VALUE_ACCESSOR.set( innerObject, pattern );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		WILDCARD_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

}
