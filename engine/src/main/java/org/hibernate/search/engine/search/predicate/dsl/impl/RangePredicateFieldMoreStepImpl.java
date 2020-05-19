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
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class RangePredicateFieldMoreStepImpl<B>
		implements RangePredicateFieldMoreStep<RangePredicateFieldMoreStepImpl<B>, RangePredicateOptionsStep<?>>,
				AbstractBooleanMultiFieldPredicateCommonState.FieldSetState<B> {

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
	public RangePredicateFieldMoreStepImpl<B> fields(String... absoluteFieldPaths) {
		return new RangePredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public RangePredicateFieldMoreStepImpl<B> boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public RangePredicateOptionsStep<?> range(Range<?> range, ValueConvert convert) {
		return commonState.range( range, convert, convert );
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
			implements RangePredicateOptionsStep<CommonState<B>> {

		CommonState(SearchPredicateBuilderFactory<?, B> builderFactory) {
			super( builderFactory );
		}

		CommonState<B> range(Range<?> range, ValueConvert lowerBoundConvert, ValueConvert upperBoundConvert) {
			Contracts.assertNotNull( range, "range" );
			if ( !range.lowerBoundValue().isPresent() && !range.upperBoundValue().isPresent() ) {
				throw log.rangePredicateCannotMatchNullValue( getEventContext() );
			}
			for ( RangePredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				for ( RangePredicateBuilder<B> predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.range( range, lowerBoundConvert, upperBoundConvert );
				}
			}
			return this;
		}

		@Override
		protected B toImplementation() {
			return super.toImplementation();
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}

}
