/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import static org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchMatchAllPredicate.MATCH_ALL_ACCESSOR;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.gson.impl.GsonUtils;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

class ElasticsearchBooleanPredicate extends AbstractElasticsearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String MUST_PROPERTY_NAME = "must";
	private static final String MUST_NOT_PROPERTY_NAME = "must_not";
	private static final String SHOULD_PROPERTY_NAME = "should";
	private static final String FILTER_PROPERTY_NAME = "filter";

	private static final JsonAccessor<String> MINIMUM_SHOULD_MATCH_ACCESSOR =
			JsonAccessor.root().property( "minimum_should_match" ).asString();

	private final List<ElasticsearchSearchPredicate> mustClauses;
	private final List<ElasticsearchSearchPredicate> mustNotClauses;
	private final List<ElasticsearchSearchPredicate> shouldClauses;
	private final List<ElasticsearchSearchPredicate> filterClauses;

	// NOTE: below modifiers (minimumShouldMatchConstraints) are used to implement hasNoModifiers() which is based on a
	// parent implementation.
	// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
	private final Map<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;

	private ElasticsearchBooleanPredicate(Builder builder) {
		super( builder );
		mustClauses = builder.mustClauses;
		mustNotClauses = builder.mustNotClauses;
		shouldClauses = builder.shouldClauses;
		filterClauses = builder.filterClauses;
		minimumShouldMatchConstraints = builder.minimumShouldMatchConstraints;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.mustClauses = null;
		builder.mustNotClauses = null;
		builder.shouldClauses = null;
		builder.filterClauses = null;
		builder.minimumShouldMatchConstraints = null;
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		checkAcceptableWithin( context, mustClauses );
		checkAcceptableWithin( context, shouldClauses );
		checkAcceptableWithin( context, filterClauses );
		checkAcceptableWithin( context, mustNotClauses );
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		contributeClauses( context, innerObject, MUST_PROPERTY_NAME, mustClauses );
		contributeClauses( context, innerObject, MUST_NOT_PROPERTY_NAME, mustNotClauses );
		contributeClauses( context, innerObject, SHOULD_PROPERTY_NAME, shouldClauses );
		contributeClauses( context, innerObject, FILTER_PROPERTY_NAME, filterClauses );

		if ( !hasAnyClauses( innerObject ) ) {
			// a "rare" case when knn predicates were added through should clauses
			// and there are no actual clauses available
			return null;
		}

		if ( isOnlyMustNot( innerObject ) && !super.hasNoModifiers() ) {
			JsonObject matchAllClause = new JsonObject();
			MATCH_ALL_ACCESSOR.set( matchAllClause, new JsonObject() );
			GsonUtils.setOrAppendToArray( innerObject, MUST_PROPERTY_NAME, matchAllClause );
		}

		if ( minimumShouldMatchConstraints != null ) {
			MINIMUM_SHOULD_MATCH_ACCESSOR.set(
					innerObject,
					ElasticsearchCommonMinimumShouldMatchConstraint
							.formatMinimumShouldMatchConstraints( minimumShouldMatchConstraints )
			);
		}

		outerObject.add( "bool", innerObject );

		return outerObject;
	}

	private void contributeClauses(PredicateRequestContext context, JsonObject innerObject,
			String occurProperty, List<ElasticsearchSearchPredicate> clauses) {
		if ( clauses == null ) {
			return;
		}

		for ( ElasticsearchSearchPredicate clause : clauses ) {
			GsonUtils.setOrAppendToArray( innerObject, occurProperty, clause.toJsonQuery( context ) );
		}
	}

	private void checkAcceptableWithin(PredicateNestingContext context, List<ElasticsearchSearchPredicate> clauses) {
		if ( clauses == null ) {
			return;
		}
		for ( ElasticsearchSearchPredicate clause : clauses ) {
			clause.checkNestableWithin( context );
		}
	}

	private boolean isOnlyMustNot(JsonObject innerObject) {
		return innerObject.has( MUST_NOT_PROPERTY_NAME )
				&& !innerObject.has( MUST_PROPERTY_NAME )
				&& !innerObject.has( SHOULD_PROPERTY_NAME )
				&& !innerObject.has( FILTER_PROPERTY_NAME );
	}

	private boolean hasAnyClauses(JsonObject innerObject) {
		return innerObject.has( MUST_NOT_PROPERTY_NAME )
				|| innerObject.has( MUST_PROPERTY_NAME )
				|| innerObject.has( SHOULD_PROPERTY_NAME )
				|| innerObject.has( FILTER_PROPERTY_NAME );
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
		return minimumShouldMatchConstraints == null
				&& super.hasNoModifiers();
	}

	static class Builder extends AbstractElasticsearchPredicate.AbstractBuilder implements BooleanPredicateBuilder {
		private List<ElasticsearchSearchPredicate> mustClauses;
		private List<ElasticsearchSearchPredicate> mustNotClauses;
		private List<ElasticsearchSearchPredicate> shouldClauses;
		private List<ElasticsearchSearchPredicate> filterClauses;

		// NOTE: below modifiers (minimumShouldMatchConstraints) are used to implement hasNoModifiers() which is based on a
		// parent implementation.
		// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
		private Map<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void must(SearchPredicate clause) {
			if ( mustClauses == null ) {
				mustClauses = new ArrayList<>();
			}
			ElasticsearchSearchPredicate elasticsearchClause = ElasticsearchSearchPredicate.from( scope, clause );
			elasticsearchClause.checkNestableWithin( PredicateNestingContext.simple() );
			mustClauses.add( elasticsearchClause );
		}

		@Override
		public void mustNot(SearchPredicate clause) {
			if ( mustNotClauses == null ) {
				mustNotClauses = new ArrayList<>();
			}
			ElasticsearchSearchPredicate elasticsearchClause = ElasticsearchSearchPredicate.from( scope, clause );
			elasticsearchClause.checkNestableWithin( PredicateNestingContext.simple() );
			mustNotClauses.add( elasticsearchClause );
		}

		@Override
		public void should(SearchPredicate clause) {
			if ( shouldClauses == null ) {
				shouldClauses = new ArrayList<>();
			}
			ElasticsearchSearchPredicate elasticsearchClause = ElasticsearchSearchPredicate.from( scope, clause );
			elasticsearchClause.checkNestableWithin( PredicateNestingContext.simple() );
			shouldClauses.add( elasticsearchClause );
		}

		@Override
		public void filter(SearchPredicate clause) {
			if ( filterClauses == null ) {
				filterClauses = new ArrayList<>();
			}
			ElasticsearchSearchPredicate elasticsearchClause = ElasticsearchSearchPredicate.from( scope, clause );
			elasticsearchClause.checkNestableWithin( PredicateNestingContext.simple() );
			filterClauses.add( elasticsearchClause );
		}

		@Override
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new ElasticsearchCommonMinimumShouldMatchConstraint( matchingClausesNumber, null )
			);
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new ElasticsearchCommonMinimumShouldMatchConstraint( null, matchingClausesPercent )
			);
		}

		@Override
		public boolean hasClause() {
			return mustClauses != null || shouldClauses != null || mustNotClauses != null || filterClauses != null;
		}

		private void addMinimumShouldMatchConstraint(int ignoreConstraintCeiling,
				ElasticsearchCommonMinimumShouldMatchConstraint constraint) {
			if ( minimumShouldMatchConstraints == null ) {
				// We'll need to go through the data in ascending order, so use a TreeMap
				minimumShouldMatchConstraints = new TreeMap<>();
			}
			Object previous = minimumShouldMatchConstraints.put( ignoreConstraintCeiling, constraint );
			if ( previous != null ) {
				throw log.minimumShouldMatchConflictingConstraints( ignoreConstraintCeiling );
			}
		}

		@Override
		public SearchPredicate build() {
			if ( !hasClause() ) {
				// HSEARCH-4619: a boolean predicate without any clause must not match anything.
				return new ElasticsearchMatchNonePredicate( this );
			}

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

			// Forcing to Lucene's defaults. See HSEARCH-3534
			if ( minimumShouldMatchConstraints == null && hasAtLeastOneMustOrFilterPredicate() ) {
				minimumShouldMatchNumber( 0, 0 );
			}

			return new ElasticsearchBooleanPredicate( this );
		}

		private void optimizeClauseCollection(List<ElasticsearchSearchPredicate> collection,
				Consumer<ElasticsearchSearchPredicate> newCollection) {
			if ( collection != null ) {
				Iterator<ElasticsearchSearchPredicate> iterator = collection.iterator();
				while ( iterator.hasNext() ) {
					ElasticsearchSearchPredicate clause = iterator.next();
					if ( clause instanceof ElasticsearchBooleanPredicate
							&& ( (ElasticsearchBooleanPredicate) clause ).hasOnlyOneMustNotClause()
							&& ( (ElasticsearchBooleanPredicate) clause ).hasNoModifiers()
					) {
						iterator.remove();
						newCollection.accept( ( (ElasticsearchBooleanPredicate) clause ).mustNotClauses.get( 0 ) );
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

		private boolean hasAtLeastOneMustOrFilterPredicate() {
			return mustClauses != null || filterClauses != null;
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
			return minimumShouldMatchConstraints == null && super.hasNoModifiers();
		}
	}

}
