/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

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
 * @param <B> The implementation type of builders.
 * @param <F> The type of field set states.
 */
abstract class AbstractBooleanMultiFieldPredicateCommonState<
		S extends AbstractBooleanMultiFieldPredicateCommonState<?, ?, ?>,
		B,
		F extends AbstractBooleanMultiFieldPredicateCommonState.FieldSetState<B>
		>
		extends AbstractPredicateFinalStep<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<F> fieldSetStates = new ArrayList<>();
	private Float predicateLevelBoost;
	private boolean withConstantScore = false;

	AbstractBooleanMultiFieldPredicateCommonState(SearchPredicateBuilderFactory<?, B> factory) {
		super( factory );
	}

	public SearchPredicateBuilderFactory<?, B> getFactory() {
		return builderFactory;
	}

	public void add(F fieldSetState) {
		fieldSetStates.add( fieldSetState );
	}

	List<F> getFieldSetStates() {
		return fieldSetStates;
	}

	public S boostedTo(float boost) {
		this.predicateLevelBoost = boost;
		return thisAsS();
	}

	public S withConstantScore() {
		withConstantScore = true;
		return thisAsS();
	}

	@Override
	protected B toImplementation() {
		List<B> predicateBuilders = new ArrayList<>();
		for ( F fieldSetState : fieldSetStates ) {
			fieldSetState.contributePredicateBuilders( predicateBuilders::add );
		}
		if ( predicateBuilders.size() > 1 ) {
			BooleanJunctionPredicateBuilder<B> boolBuilder = builderFactory.bool();
			for ( B predicateBuilder : predicateBuilders ) {
				boolBuilder.should( predicateBuilder );
			}
			return boolBuilder.toImplementation();
		}
		else {
			return predicateBuilders.get( 0 );
		}
	}

	protected abstract S thisAsS();

	final void applyBoostAndConstantScore(Float fieldSetBoost, SearchPredicateBuilder<?> predicateBuilder) {
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
			predicateBuilder.withConstantScore();
		}
	}

	protected final EventContext getEventContext() {
		return EventContexts.fromIndexFieldAbsolutePaths(
				getFieldSetStates().stream().flatMap( f -> f.getAbsoluteFieldPaths().stream() )
						.collect( Collectors.toList() )
		);
	}

	public interface FieldSetState<B> {
		List<String> getAbsoluteFieldPaths();
		void contributePredicateBuilders(Consumer<B> collector);
	}
}