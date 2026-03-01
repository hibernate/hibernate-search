/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

class LuceneBooleanPredicate extends AbstractLuceneSearchPredicate {

	private final List<LuceneSearchPredicate> mustClauses;
	private final List<LuceneSearchPredicate> mustNotClauses;
	private final List<LuceneSearchPredicate> shouldClauses;
	private final List<LuceneSearchPredicate> filterClauses;

	// NOTE: below modifiers (minimumShouldMatchConstraints) are used to implement hasNoModifiers() which is based on a
	// parent implementation.
	// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
	private final LuceneCommonMinimumShouldMatchConstraints minimumShouldMatchConstraint;

	private LuceneBooleanPredicate(Builder builder) {
		super( builder );
		mustClauses = builder.mustClauses;
		mustNotClauses = builder.mustNotClauses;
		shouldClauses = builder.shouldClauses;
		filterClauses = builder.filterClauses;
		minimumShouldMatchConstraint = builder.minimumShouldMatchBuilder;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.mustClauses = null;
		builder.shouldClauses = null;
		builder.mustNotClauses = null;
		builder.filterClauses = null;
		builder.minimumShouldMatchBuilder = null;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		checkNestableWithin( expectedParentNestedPath, mustClauses );
		checkNestableWithin( expectedParentNestedPath, shouldClauses );
		checkNestableWithin( expectedParentNestedPath, filterClauses );
		checkNestableWithin( expectedParentNestedPath, mustNotClauses );
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		contributeQueries( context, booleanQueryBuilder, mustClauses, Occur.MUST );
		contributeQueries( context, booleanQueryBuilder, mustNotClauses, Occur.MUST_NOT );
		contributeQueries( context, booleanQueryBuilder, shouldClauses, Occur.SHOULD );
		contributeQueries( context, booleanQueryBuilder, filterClauses, Occur.FILTER );

		if ( isOnlyMustNot() ) {
			booleanQueryBuilder.add( MatchAllDocsQuery.INSTANCE, super.hasNoModifiers() ? Occur.FILTER : Occur.MUST );
		}

		if ( !minimumShouldMatchConstraint.isEmpty() && shouldClauses != null ) {
			int minimumShouldMatch = minimumShouldMatchConstraint.minimumShouldMatch( shouldClauses );
			booleanQueryBuilder.setMinimumNumberShouldMatch( minimumShouldMatch );
		}

		return booleanQueryBuilder.build();
	}

	private void contributeQueries(PredicateRequestContext context, BooleanQuery.Builder booleanQueryBuilder,
			List<LuceneSearchPredicate> clauses, Occur occur) {
		if ( clauses == null ) {
			return;
		}

		for ( LuceneSearchPredicate clause : clauses ) {
			booleanQueryBuilder.add( clause.toQuery( context ), occur );
		}
	}

	private void checkNestableWithin(String expectedParentNestedPath, List<LuceneSearchPredicate> clauses) {
		if ( clauses == null ) {
			return;
		}
		for ( LuceneSearchPredicate clause : clauses ) {
			clause.checkNestableWithin( expectedParentNestedPath );
		}
	}

	private boolean isOnlyMustNot() {
		return mustNotClauses != null
				&& !mustNotClauses.isEmpty()
				&& ( mustClauses == null || mustClauses.isEmpty() )
				&& ( shouldClauses == null || shouldClauses.isEmpty() )
				&& ( filterClauses == null || filterClauses.isEmpty() );
	}

	private boolean hasOnlyOneMustNotClause() {
		return isOnlyMustNot() && mustNotClauses.size() == 1;
	}

	@Override
	protected boolean hasNoModifiers() {
		return minimumShouldMatchConstraint.isEmpty()
				&& super.hasNoModifiers();
	}

	static class Builder extends AbstractBuilder implements BooleanPredicateBuilder {
		private List<LuceneSearchPredicate> mustClauses;
		private List<LuceneSearchPredicate> mustNotClauses;
		private List<LuceneSearchPredicate> shouldClauses;
		private List<LuceneSearchPredicate> filterClauses;

		// NOTE: below modifiers (minimumShouldMatchConstraints) are used to implement hasNoModifiers() which is based on a
		// parent implementation.
		// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
		private LuceneCommonMinimumShouldMatchConstraints minimumShouldMatchBuilder;

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
			minimumShouldMatchBuilder = new LuceneCommonMinimumShouldMatchConstraints();
		}

		@Override
		public void must(SearchPredicate clause) {
			if ( mustClauses == null ) {
				mustClauses = new ArrayList<>();
			}
			mustClauses.add( LuceneSearchPredicate.from( scope, clause ) );
		}

		@Override
		public void mustNot(SearchPredicate clause) {
			if ( mustNotClauses == null ) {
				mustNotClauses = new ArrayList<>();
			}
			mustNotClauses.add( LuceneSearchPredicate.from( scope, clause ) );
		}

		@Override
		public void should(SearchPredicate clause) {
			if ( shouldClauses == null ) {
				shouldClauses = new ArrayList<>();
			}
			shouldClauses.add( LuceneSearchPredicate.from( scope, clause ) );
		}

		@Override
		public void filter(SearchPredicate clause) {
			if ( filterClauses == null ) {
				filterClauses = new ArrayList<>();
			}
			filterClauses.add( LuceneSearchPredicate.from( scope, clause ) );
		}

		@Override
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			minimumShouldMatchBuilder.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			minimumShouldMatchBuilder.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
		}

		@Override
		public boolean hasClause() {
			return mustClauses != null || shouldClauses != null || mustNotClauses != null || filterClauses != null;
		}

		@Override
		public SearchPredicate build() {
			optimizeClauseCollection(
					mustClauses,
					this::mustNot
			);

			optimizeClauseCollection(
					mustNotClauses,
					this::must
			);

			checkAndClearClauseCollections();

			if ( hasNoModifiers() ) {
				if ( hasOnlyOneMustClause() ) {
					return mustClauses.get( 0 );
				}
				else if ( hasOnlyOneShouldClause() ) {
					return shouldClauses.get( 0 );
				}
			}

			return new LuceneBooleanPredicate( this );
		}

		private void optimizeClauseCollection(List<LuceneSearchPredicate> collection,
				Consumer<LuceneSearchPredicate> newCollection) {
			if ( collection != null ) {
				Iterator<LuceneSearchPredicate> iterator = collection.iterator();
				while ( iterator.hasNext() ) {
					LuceneSearchPredicate clause = iterator.next();
					if ( clause instanceof LuceneBooleanPredicate
							&& ( (LuceneBooleanPredicate) clause ).hasOnlyOneMustNotClause()
							&& ( (LuceneBooleanPredicate) clause ).hasNoModifiers()
					) {
						iterator.remove();
						newCollection.accept( ( (LuceneBooleanPredicate) clause ).mustNotClauses.get( 0 ) );
					}
				}
			}
		}

		private void checkAndClearClauseCollections() {
			if ( mustClauses != null && mustClauses.isEmpty() ) {
				mustClauses = null;
			}
			if ( mustNotClauses != null && mustNotClauses.isEmpty() ) {
				mustNotClauses = null;
			}
		}

		private boolean hasOnlyOneMustClause() {
			return mustClauses != null
					&& mustClauses.size() == 1
					&& ( mustNotClauses == null || mustNotClauses.isEmpty() )
					&& ( shouldClauses == null || shouldClauses.isEmpty() )
					&& ( filterClauses == null || filterClauses.isEmpty() );
		}

		private boolean hasOnlyOneShouldClause() {
			return shouldClauses != null
					&& shouldClauses.size() == 1
					&& ( mustNotClauses == null || mustNotClauses.isEmpty() )
					&& ( mustClauses == null || mustClauses.isEmpty() )
					&& ( filterClauses == null || filterClauses.isEmpty() );
		}

		@Override
		protected boolean hasNoModifiers() {
			return minimumShouldMatchBuilder.isEmpty() && super.hasNoModifiers();
		}
	}

}
