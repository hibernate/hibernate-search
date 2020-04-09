/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import com.google.gson.JsonObject;


class ElasticsearchNestedPredicateBuilder extends AbstractElasticsearchSingleFieldPredicateBuilder
		implements NestedPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonAccessor<String> PATH_ACCESSOR = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asObject();

	private ElasticsearchSearchPredicateBuilder nestedBuilder;

	ElasticsearchNestedPredicateBuilder(String absoluteFieldPath, List<String> nestedPathHierarchy) {
		super(
				absoluteFieldPath,
				// The given list includes absoluteFieldPath at the end, but here we don't want it to be included.
				nestedPathHierarchy.subList( 0, nestedPathHierarchy.size() - 1 )
		);
	}

	@Override
	public void nested(ElasticsearchSearchPredicateBuilder nestedBuilder) {
		nestedBuilder.checkNestableWithin( absoluteFieldPath );
		this.nestedBuilder = nestedBuilder;
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		ElasticsearchSearchPredicateContext nestedContext = context.withNestedPath( absoluteFieldPath );

		PATH_ACCESSOR.set( innerObject, absoluteFieldPath );
		QUERY_ACCESSOR.set( innerObject, nestedBuilder.build( nestedContext ) );
		outerObject.add( "nested", innerObject );
		return outerObject;
	}
}
