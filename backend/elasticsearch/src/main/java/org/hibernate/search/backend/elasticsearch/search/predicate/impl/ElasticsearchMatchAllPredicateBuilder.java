/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;

import com.google.gson.JsonObject;


class ElasticsearchMatchAllPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder
		implements MatchAllPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor MATCH_ALL_ACCESSOR = JsonAccessor.root().property( "match_all" ).asObject();

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		MATCH_ALL_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

}
