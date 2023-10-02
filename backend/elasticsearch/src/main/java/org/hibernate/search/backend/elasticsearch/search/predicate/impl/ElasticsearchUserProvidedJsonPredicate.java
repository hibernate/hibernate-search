/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import com.google.gson.JsonObject;

class ElasticsearchUserProvidedJsonPredicate implements ElasticsearchSearchPredicate {

	private final Set<String> indexNames;
	private final JsonObject json;

	ElasticsearchUserProvidedJsonPredicate(ElasticsearchSearchIndexScope<?> scope,
			JsonObject json) {
		indexNames = scope.hibernateSearchIndexNames();
		this.json = json;
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		// Nothing to do: we'll assume the user knows what they are doing.
	}

	@Override
	public JsonObject toJsonQuery(PredicateRequestContext context) {
		return json;
	}
}
