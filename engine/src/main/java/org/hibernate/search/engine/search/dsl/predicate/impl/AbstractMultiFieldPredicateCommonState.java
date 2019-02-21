/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

abstract class AbstractMultiFieldPredicateCommonState<B, F extends AbstractMultiFieldPredicateCommonState.FieldSetContext<B>>
		extends AbstractSearchPredicateTerminalContext<B> {

	private final List<F> fieldSetContexts = new ArrayList<>();
	private Float predicateLevelBoost;
	private boolean withConstantScore = false;

	AbstractMultiFieldPredicateCommonState(SearchPredicateBuilderFactory<?, B> factory) {
		super( factory );
	}

	public SearchPredicateBuilderFactory<?, B> getFactory() {
		return factory;
	}

	public void add(F fieldSetContext) {
		fieldSetContexts.add( fieldSetContext );
	}

	List<F> getFieldSetContexts() {
		return fieldSetContexts;
	}

	void setPredicateLevelBoost(Float boost) {
		this.predicateLevelBoost = boost;
	}

	void applyBoostAndConstantScore(Float fieldBoost, List<? extends SearchPredicateBuilder> predicateBuilders) {
		for ( SearchPredicateBuilder predicateBuilder : predicateBuilders ) {
			applyBoostAndConstantScore( fieldBoost, predicateBuilder );
		}
	}

	void withConstantScore() {
		withConstantScore = true;
	}

	void applyBoostAndConstantScore(Float fieldBoost, SearchPredicateBuilder predicateBuilder) {
		if ( predicateLevelBoost != null && fieldBoost != null ) {
			predicateBuilder.boost( predicateLevelBoost * fieldBoost );
		}
		else if ( predicateLevelBoost != null ) {
			predicateBuilder.boost( predicateLevelBoost );
		}
		else if ( fieldBoost != null ) {
			predicateBuilder.boost( fieldBoost );
		}

		if ( withConstantScore ) {
			predicateBuilder.withConstantScore();
		}
	}

	@Override
	protected B toImplementation() {
		List<B> predicateBuilders = new ArrayList<>();
		for ( F fieldSetContext : fieldSetContexts ) {
			fieldSetContext.contributePredicateBuilders( predicateBuilders::add );
		}
		if ( predicateBuilders.size() > 1 ) {
			BooleanJunctionPredicateBuilder<B> boolBuilder = factory.bool();
			for ( B predicateBuilder : predicateBuilders ) {
				boolBuilder.should( predicateBuilder );
			}
			return boolBuilder.toImplementation();
		}
		else {
			return predicateBuilders.get( 0 );
		}
	}

	public interface FieldSetContext<B> {
		void contributePredicateBuilders(Consumer<B> collector);
	}
}