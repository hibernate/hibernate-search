/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import com.google.gson.JsonObject;


class ElasticsearchUserProvidedJsonPredicateBuilder implements ElasticsearchSearchPredicateBuilder {

	private final ElasticsearchSearchContext searchContext;
	private final JsonObject json;

	ElasticsearchUserProvidedJsonPredicateBuilder(ElasticsearchSearchContext searchContext, JsonObject json) {
		this.searchContext = searchContext;
		this.json = json;
	}

	@Override
	public SearchPredicate build() {
		return ElasticsearchSearchPredicate.of( searchContext, this );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do: we'll assume the user knows what they are doing.
	}

	@Override
	public JsonObject toJsonQuery(PredicateRequestContext context) {
		return json;
	}

}
