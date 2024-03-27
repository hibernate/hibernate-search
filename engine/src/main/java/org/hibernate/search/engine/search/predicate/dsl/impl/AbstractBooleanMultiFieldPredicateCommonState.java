/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A common state for a multi-field predicate DSL
 * that will simply create one predicate per field and a boolean query to join the predicates.
 * <p>
 * This abstract class is appropriate if the predicate supports targeting multiple fields at the DSL level,
 * but not at the backend SPI level (like for range predicates, for example).
 * Some predicate support targeting multiple fields at the backend SPI level,
 * like the simple query string predicate.
 *
 * @param <S> The "self" type returned by DSL methods.
 * @param <F> The type of field set states.
 */
abstract class AbstractBooleanMultiFieldPredicateCommonState<
		S extends AbstractBooleanMultiFieldPredicateCommonState<?, ?>,
		F extends AbstractBooleanMultiFieldPredicateCommonState.FieldSetState>
		extends AbstractPredicateFinalStep {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<F> fieldSetStates = new ArrayList<>();
	private Float predicateLevelBoost;
	private boolean withConstantScore = false;

	AbstractBooleanMultiFieldPredicateCommonState(SearchPredicateDslContext<?> dslContext) {
		super( dslContext );
	}

	public SearchIndexScope<?> scope() {
		return dslContext.scope();
	}

	public void add(F fieldSetState) {
		fieldSetStates.add( fieldSetState );
	}

	List<F> getFieldSetStates() {
		return fieldSetStates;
	}

	public S boost(float boost) {
		this.predicateLevelBoost = boost;
		return thisAsS();
	}

	public S constantScore() {
		withConstantScore = true;
		return thisAsS();
	}

	@Override
	protected SearchPredicate build() {
		List<SearchPredicate> predicates = new ArrayList<>();
		for ( F fieldSetState : fieldSetStates ) {
			fieldSetState.contributePredicates( predicates::add );
		}
		if ( predicates.size() > 1 ) {
			BooleanPredicateBuilder boolBuilder = dslContext.scope().predicateBuilders().bool();
			for ( SearchPredicate predicate : predicates ) {
				boolBuilder.should( predicate );
			}
			return boolBuilder.build();
		}
		else {
			return predicates.get( 0 );
		}
	}

	protected abstract S thisAsS();

	final void applyBoostAndConstantScore(Float fieldSetBoost, SearchPredicateBuilder predicateBuilder) {
		if ( fieldSetBoost != null && withConstantScore ) {
			// another good option would be the one to simply ignore the fieldSetBoost
			// when the option withConstantScore is defined
			throw log.perFieldBoostWithConstantScore();
		}
		if ( predicateLevelBoost != null && fieldSetBoost != null ) {
			predicateBuilder.boost( predicateLevelBoost * fieldSetBoost );
		}
		else if ( predicateLevelBoost != null ) {
			predicateBuilder.boost( predicateLevelBoost );
		}
		else if ( fieldSetBoost != null ) {
			predicateBuilder.boost( fieldSetBoost );
		}

		if ( withConstantScore ) {
			predicateBuilder.constantScore();
		}
	}

	public interface FieldSetState {
		void contributePredicates(Consumer<SearchPredicate> collector);
	}
}
