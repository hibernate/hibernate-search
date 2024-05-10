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
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class RangePredicateFieldMoreStepImpl
		implements RangePredicateFieldMoreStep<RangePredicateFieldMoreStepImpl, RangePredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState commonState;

	private final List<String> fieldPaths;
	private final List<RangePredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	RangePredicateFieldMoreStepImpl(CommonState commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.fieldPaths = fieldPaths;
		for ( String path : fieldPaths ) {
			// only check that the range predicate can be applied to the requested field:
			commonState.scope().fieldQueryElement( path, PredicateTypeKeys.RANGE );
		}
	}

	@Override
	public RangePredicateFieldMoreStepImpl fields(String... fieldPaths) {
		return new RangePredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public RangePredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public RangePredicateOptionsStep<?> within(Range<?> range, ValueConvert convert) {
		return commonState.within( range, convert, convert );
	}

	@Override
	public RangePredicateOptionsStep<?> withinAny(Collection<? extends Range<?>> ranges, ValueConvert convert) {
		return commonState.withinAny( ranges, convert );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( RangePredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, RangePredicateFieldMoreStepImpl>
			implements RangePredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		CommonState within(Range<?> range, ValueConvert lowerBoundConvert, ValueConvert upperBoundConvert) {
			Contracts.assertNotNull( range, "range" );
			Contracts.assertNotNull( lowerBoundConvert, "lowerBoundConvert" );
			Contracts.assertNotNull( upperBoundConvert, "upperBoundConvert" );
			if ( range.lowerBoundValue().isEmpty() && range.upperBoundValue().isEmpty() ) {
				throw log.rangePredicateCannotMatchNullValue( getEventContext() );
			}
			for ( RangePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( String path : fieldSetState.fieldPaths ) {
					RangePredicateBuilder builder = scope().fieldQueryElement( path, PredicateTypeKeys.RANGE );
					builder.within( range, lowerBoundConvert, upperBoundConvert );
					fieldSetState.predicateBuilders.add( builder );
				}
			}
			return this;
		}

		public CommonState withinAny(Collection<? extends Range<?>> ranges, ValueConvert valueConvert) {
			Contracts.assertNotNull( valueConvert, "valueConvert" );
			for ( RangePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( String path : fieldSetState.fieldPaths ) {
					for ( var range : ranges ) {
						Contracts.assertNotNull( range, "range" );
						if ( range.lowerBoundValue().isEmpty() && range.upperBoundValue().isEmpty() ) {
							throw log.rangePredicateCannotMatchNullValue( getEventContext() );
						}
						RangePredicateBuilder builder = scope().fieldQueryElement( path, PredicateTypeKeys.RANGE );
						builder.within( range, valueConvert, valueConvert );
						fieldSetState.predicateBuilders.add( builder );
					}
				}
			}

			return this;
		}

		@Override
		protected CommonState thisAsS() {
			return this;
		}

		protected final EventContext getEventContext() {
			return EventContexts.fromIndexFieldAbsolutePaths(
					getFieldSetStates().stream().flatMap( f -> f.fieldPaths.stream() )
							.collect( Collectors.toList() )
			);
		}
	}

}
