/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchPredicate extends SearchPredicate {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	void checkNestableWithin(PredicateNestingContext context);

	JsonObject toJsonQuery(PredicateRequestContext context);

	static ElasticsearchSearchPredicate from(ElasticsearchSearchIndexScope<?> scope, SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}
		ElasticsearchSearchPredicate casted = (ElasticsearchSearchPredicate) predicate;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.indexNames(),
					scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() )
			);
		}
		return casted;
	}
}
