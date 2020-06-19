/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;

import com.google.gson.JsonObject;


class ElasticsearchMatchAllPredicate extends AbstractElasticsearchPredicate {

	private static final JsonObjectAccessor MATCH_ALL_ACCESSOR = JsonAccessor.root().property( "match_all" ).asObject();

	private ElasticsearchMatchAllPredicate(Builder builder) {
		super( builder );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		MATCH_ALL_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	static class Builder extends AbstractElasticsearchPredicate.AbstractBuilder implements MatchAllPredicateBuilder {
		Builder(ElasticsearchSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchMatchAllPredicate( this );
		}
	}
}
