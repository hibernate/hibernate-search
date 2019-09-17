/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateLastLimitExcludeStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFromToStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.common.impl.Contracts;
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
	public RangePredicateFieldMoreStep fields(String... absoluteFieldPaths) {
		return new RangePredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public RangePredicateFieldMoreStep boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public RangePredicateOptionsStep range(Range<?> range, ValueConvert convert) {
		return commonState.range( range, convert, convert );
	}

	@Override
	public RangePredicateFromToStep from(Object value, ValueConvert convert) {
		commonState.getOrCreateLegacySyntaxState().lowerBoundValue = value;
		commonState.getOrCreateLegacySyntaxState().lowerBoundConvert = convert;
		return new RangePredicateFromToStepImpl<>( commonState );
	}

	@Override
	public RangePredicateLastLimitExcludeStep above(Object value, ValueConvert convert) {
		commonState.getOrCreateLegacySyntaxState().lowerBoundValue = value;
		commonState.getOrCreateLegacySyntaxState().lowerBoundConvert = convert;
		return new RangePredicateSingleLimitExcludeStep<>( commonState, false );
	}

	@Override
	public RangePredicateLastLimitExcludeStep below(Object value, ValueConvert convert) {
		commonState.getOrCreateLegacySyntaxState().upperBoundValue = value;
		commonState.getOrCreateLegacySyntaxState().upperBoundConvert = convert;
		return new RangePredicateSingleLimitExcludeStep<>( commonState, true );
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

	static class CommonState<B>
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<B>, B, RangePredicateFieldMoreStepImpl<B>>
			implements RangePredicateOptionsStep {

		private LegacySyntaxState legacySyntaxState;

		CommonState(SearchPredicateBuilderFactory<?, B> builderFactory) {
			super( builderFactory );
		}

		RangePredicateOptionsStep range(Range<?> range, ValueConvert lowerBoundConvert, ValueConvert upperBoundConvert) {
			Contracts.assertNotNull( range, "range" );
			if ( !range.getLowerBoundValue().isPresent() && !range.getUpperBoundValue().isPresent() ) {
				throw log.rangePredicateCannotMatchNullValue( getEventContext() );
			}
			for ( RangePredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				for ( RangePredicateBuilder<B> predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.range( range, lowerBoundConvert, upperBoundConvert );
				}
			}
			return this;
		}

		LegacySyntaxState getOrCreateLegacySyntaxState() {
			if ( legacySyntaxState == null ) {
				legacySyntaxState = new LegacySyntaxState();
			}
			return legacySyntaxState;
		}

		@Override
		protected B toImplementation() {
			if ( legacySyntaxState != null ) {
				legacySyntaxState.contribute( this );
			}
			return super.toImplementation();
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}

	static class RangePredicateFromToStepImpl<B> implements RangePredicateFromToStep {

		private final CommonState<B> commonState;

		RangePredicateFromToStepImpl(CommonState<B> commonState) {
			this.commonState = commonState;
		}

		@Override
		public RangePredicateLastLimitExcludeStep to(Object value, ValueConvert convert) {
			commonState.getOrCreateLegacySyntaxState().upperBoundValue = value;
			commonState.getOrCreateLegacySyntaxState().upperBoundConvert = convert;
			return new RangePredicateSingleLimitExcludeStep<>( commonState, true );
		}

		@Override
		public RangePredicateFromToStep excludeLimit() {
			commonState.getOrCreateLegacySyntaxState().lowerBoundInclusion = RangeBoundInclusion.EXCLUDED;
			return this;
		}
	}

	static class RangePredicateSingleLimitExcludeStep<B> implements RangePredicateLastLimitExcludeStep {

		private final CommonState<B> commonState;

		private final boolean isUpperBound;

		RangePredicateSingleLimitExcludeStep(CommonState<B> commonState, boolean isUpperBound) {
			this.commonState = commonState;
			this.isUpperBound = isUpperBound;
		}

		@Override
		public RangePredicateOptionsStep boost(float boost) {
			commonState.boost( boost );
			return this;
		}

		@Override
		public RangePredicateOptionsStep excludeLimit() {
			if ( isUpperBound ) {
				commonState.getOrCreateLegacySyntaxState().upperBoundInclusion = RangeBoundInclusion.EXCLUDED;
			}
			else {
				commonState.getOrCreateLegacySyntaxState().lowerBoundInclusion = RangeBoundInclusion.EXCLUDED;
			}
			return this;
		}

		@Override
		public SearchPredicate toPredicate() {
			return commonState.toPredicate();
		}
	}

	private static class LegacySyntaxState {
		private Object lowerBoundValue = null;
		private RangeBoundInclusion lowerBoundInclusion = RangeBoundInclusion.INCLUDED;
		private ValueConvert lowerBoundConvert = ValueConvert.YES;
		private Object upperBoundValue = null;
		private RangeBoundInclusion upperBoundInclusion = RangeBoundInclusion.INCLUDED;
		private ValueConvert upperBoundConvert = ValueConvert.YES;

		public <B> void contribute(CommonState<B> commonState) {
			commonState.range(
					Range.between(
							lowerBoundValue, lowerBoundInclusion,
							upperBoundValue, upperBoundInclusion
					),
					lowerBoundConvert,
					upperBoundConvert
			);
		}
	}
}
