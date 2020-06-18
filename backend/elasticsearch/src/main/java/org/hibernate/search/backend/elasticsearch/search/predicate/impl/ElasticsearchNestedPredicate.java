/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import com.google.gson.JsonObject;


class ElasticsearchNestedPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonAccessor<String> PATH_ACCESSOR = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asObject();

	private final ElasticsearchSearchPredicate nestedPredicate;

	private ElasticsearchNestedPredicate(Builder builder) {
		super( builder );
		nestedPredicate = builder.nestedPredicate;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		PredicateRequestContext nestedContext = context.withNestedPath( absoluteFieldPath );

		PATH_ACCESSOR.set( innerObject, absoluteFieldPath );
		QUERY_ACCESSOR.set( innerObject, nestedPredicate.toJsonQuery( nestedContext ) );
		outerObject.add( "nested", innerObject );
		return outerObject;
	}

	static class Builder extends AbstractBuilder implements NestedPredicateBuilder {
		private ElasticsearchSearchPredicate nestedPredicate;

		Builder(ElasticsearchSearchContext searchContext, String absoluteFieldPath,
				List<String> nestedPathHierarchy) {
			// The given list includes absoluteFieldPath at the end, but here we don't want it to be included.
			super( searchContext, absoluteFieldPath, nestedPathHierarchy.subList( 0, nestedPathHierarchy.size() - 1 ) );
		}

		@Override
		public void nested(SearchPredicate nestedPredicate) {
			ElasticsearchSearchPredicate elasticsearchPredicate = ElasticsearchSearchPredicate.from( searchContext, nestedPredicate );
			elasticsearchPredicate.checkNestableWithin( absoluteFieldPath );
			this.nestedPredicate = elasticsearchPredicate;
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchNestedPredicate( this );
		}
	}
}
