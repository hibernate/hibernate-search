/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

import com.google.gson.JsonObject;

class ElasticsearchSearchPredicate
		implements SearchPredicate, ElasticsearchSearchPredicateBuilder {

	private final ElasticsearchSearchPredicateBuilder delegate;
	private final Set<String> indexNames;

	ElasticsearchSearchPredicate(ElasticsearchSearchPredicateBuilder delegate, Set<String> indexNames) {
		this.delegate = delegate;
		this.indexNames = indexNames;
	}

	@Override
	public JsonObject build(ElasticsearchSearchPredicateContext context) {
		return delegate.build( context );
	}

	public Set<String> getIndexNames() {
		return indexNames;
	}
}
