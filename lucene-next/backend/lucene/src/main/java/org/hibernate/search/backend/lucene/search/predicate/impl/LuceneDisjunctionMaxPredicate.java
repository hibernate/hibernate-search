/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.DisjunctionMaxPredicateBuilder;

import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;

class LuceneDisjunctionMaxPredicate extends AbstractLuceneSearchPredicate {

	private final List<LuceneSearchPredicate> disjuncts;
	private final float tieBreaker;

	private LuceneDisjunctionMaxPredicate(Builder builder) {
		super( builder );
		disjuncts = builder.disjuncts;
		tieBreaker = builder.tieBreaker;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.disjuncts = null;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		for ( LuceneSearchPredicate disjunct : disjuncts ) {
			disjunct.checkNestableWithin( expectedParentNestedPath );
		}
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		List<Query> queries = new ArrayList<>( disjuncts.size() );
		for ( LuceneSearchPredicate disjunct : disjuncts ) {
			queries.add( disjunct.toQuery( context ) );
		}
		return new DisjunctionMaxQuery( queries, tieBreaker );
	}

	static class Builder extends AbstractBuilder implements DisjunctionMaxPredicateBuilder {

		private List<LuceneSearchPredicate> disjuncts = new ArrayList<>();
		private float tieBreaker;

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void disjunct(SearchPredicate clause) {
			LuceneSearchPredicate luceneClause = LuceneSearchPredicate.from( scope, clause );
			disjuncts.add( luceneClause );
		}

		@Override
		public void tieBreaker(float tieBreaker) {
			this.tieBreaker = tieBreaker;
		}

		@Override
		public boolean hasClause() {
			return !disjuncts.isEmpty();
		}

		@Override
		public SearchPredicate build() {
			return new LuceneDisjunctionMaxPredicate( this );
		}
	}
}
