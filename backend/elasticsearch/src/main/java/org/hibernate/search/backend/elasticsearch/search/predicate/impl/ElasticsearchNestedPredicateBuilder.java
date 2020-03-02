/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import com.google.gson.JsonObject;


class ElasticsearchNestedPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder
		implements NestedPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonAccessor<String> PATH_ACCESSOR = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asObject();

	private final String absoluteFieldPath;

	private ElasticsearchSearchPredicateBuilder nestedBuilder;

	ElasticsearchNestedPredicateBuilder(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public void nested(ElasticsearchSearchPredicateBuilder nestedBuilder) {
		this.nestedBuilder = nestedBuilder;
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		ElasticsearchSearchPredicateContext nestedContext = context.explicitNested( absoluteFieldPath );

		PATH_ACCESSOR.set( innerObject, absoluteFieldPath );
		QUERY_ACCESSOR.set( innerObject, nestedBuilder.build( nestedContext ) );
		outerObject.add( "nested", innerObject );
		return outerObject;
	}

}
