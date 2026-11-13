package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import tools.jackson.databind.node.ObjectNode;

class ElasticsearchUserProvidedJsonPredicate implements ElasticsearchSearchPredicate {

	private final Set<String> indexNames;
	private final ObjectNode json;

	ElasticsearchUserProvidedJsonPredicate(ElasticsearchSearchIndexScope<?> scope,
			ObjectNode json) {
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
	public ObjectNode toJsonQuery(PredicateRequestContext context) {
		return json;
	}
}
