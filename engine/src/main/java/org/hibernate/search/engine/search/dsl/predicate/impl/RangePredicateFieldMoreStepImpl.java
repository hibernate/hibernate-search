/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateLastLimitExcludeStep;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFromToStep;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class RangePredicateFieldMoreStepImpl<B>
		implements RangePredicateFieldMoreStep, AbstractBooleanMultiFieldPredicateCommonState.FieldSetState<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<RangePredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	RangePredicateFieldMoreStepImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.range( absoluteFieldPath ) );
		}
	}

	@Override
	public RangePredicateFieldMoreStep orFields(String... absoluteFieldPaths) {
		return new RangePredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public RangePredicateFieldMoreStep boostedTo(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public RangePredicateFromToStep from(Object value, DslConverter dslConverter) {
		return commonState.from( value, dslConverter );
	}

	@Override
	public RangePredicateLastLimitExcludeStep above(Object value, DslConverter dslConverter) {
		return commonState.above( value, dslConverter );
	}

	@Override
	public RangePredicateLastLimitExcludeStep below(Object value, DslConverter dslConverter) {
		return commonState.below( value, dslConverter );
	}

	@Override
	public List<String> getAbsoluteFieldPaths() {
		return absoluteFieldPaths;
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( RangePredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<B>, B, RangePredicateFieldMoreStepImpl<B>>
			implements RangePredicateLastLimitExcludeStep {

		private boolean hasNonNullBound = false;

		// excludeLimit in from/above means excluding the lower limit
		// excludeLimit in to/below means excluding the upper one
		protected boolean excludeUpperLimit = false;

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
		}

		@Override
		public RangePredicateOptionsStep excludeLimit() {
			getQueryBuilders().forEach( ( excludeUpperLimit ) ? RangePredicateBuilder::excludeUpperLimit : RangePredicateBuilder::excludeLowerLimit );
			return this;
		}

		@Override
		protected B toImplementation() {
			// Just in case from() was called, but not to()
			checkHasNonNullBound();
			return super.toImplementation();
		}

		RangePredicateFromToStep from(Object value, DslConverter dslConverter) {
			doAbove( value, dslConverter );
			return new RangePredicateFromToStepImpl<>( this );
		}

		RangePredicateLastLimitExcludeStep above(Object value, DslConverter dslConverter) {
			doAbove( value, dslConverter );
			checkHasNonNullBound();
			return this;
		}

		RangePredicateLastLimitExcludeStep below(Object value, DslConverter dslConverter) {
			doBelow( value, dslConverter );
			checkHasNonNullBound();
			return this;
		}

		private void doAbove(Object value, DslConverter dslConverter) {
			excludeUpperLimit = false;
			if ( value != null ) {
				hasNonNullBound = true;
				getQueryBuilders().forEach( q -> q.lowerLimit( value, dslConverter ) );
			}
		}

		private void doBelow(Object value, DslConverter dslConverter) {
			excludeUpperLimit = true;
			if ( value != null ) {
				hasNonNullBound = true;
				getQueryBuilders().forEach( q -> q.upperLimit( value, dslConverter ) );
			}
		}

		private void checkHasNonNullBound() {
			if ( !hasNonNullBound ) {
				throw log.rangePredicateCannotMatchNullValue( getEventContext() );
			}
		}

		private Stream<RangePredicateBuilder<B>> getQueryBuilders() {
			return getFieldSetStates().stream().flatMap( f -> f.predicateBuilders.stream() );
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}

	static class RangePredicateFromToStepImpl<B> implements RangePredicateFromToStep {

		private final CommonState<B> delegate;

		RangePredicateFromToStepImpl(CommonState<B> delegate) {
			this.delegate = delegate;
		}

		@Override
		public RangePredicateLastLimitExcludeStep to(Object value, DslConverter dslConverter) {
			delegate.doBelow( value, dslConverter );
			delegate.checkHasNonNullBound();
			return delegate;
		}

		@Override
		public RangePredicateFromToStep excludeLimit() {
			delegate.getQueryBuilders().forEach( RangePredicateBuilder::excludeLowerLimit );
			return this;
		}
	}
}
