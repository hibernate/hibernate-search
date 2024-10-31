/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.QueryLog;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.reporting.EventContext;

class RangePredicateFieldMoreStepImpl
		implements RangePredicateFieldMoreStep<RangePredicateFieldMoreStepImpl, RangePredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

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
	public RangePredicateOptionsStep<?> within(Range<?> range, ValueModel valueModel) {
		return commonState.within( range, valueModel, valueModel );
	}

	@Override
	public RangePredicateOptionsStep<?> withinAny(Collection<? extends Range<?>> ranges, ValueModel valueModel) {
		return commonState.withinAny( ranges, valueModel );
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

		CommonState within(Range<?> range, ValueModel lowerBoundModel, ValueModel upperBoundModel) {
			Contracts.assertNotNull( range, "range" );
			Contracts.assertNotNull( lowerBoundModel, "lowerBoundModel" );
			Contracts.assertNotNull( upperBoundModel, "upperBoundModel" );
			if ( range.lowerBoundValue().isEmpty() && range.upperBoundValue().isEmpty() ) {
				throw QueryLog.INSTANCE.rangePredicateCannotMatchNullValue( getEventContext() );
			}
			for ( RangePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( String path : fieldSetState.fieldPaths ) {
					RangePredicateBuilder builder = scope().fieldQueryElement( path, PredicateTypeKeys.RANGE );
					builder.within( range, lowerBoundModel, upperBoundModel );
					fieldSetState.predicateBuilders.add( builder );
				}
			}
			return this;
		}

		public CommonState withinAny(Collection<? extends Range<?>> ranges, ValueModel valueModel) {
			Contracts.assertNotNull( valueModel, "valueModel" );
			for ( RangePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( String path : fieldSetState.fieldPaths ) {
					for ( var range : ranges ) {
						Contracts.assertNotNull( range, "range" );
						if ( range.lowerBoundValue().isEmpty() && range.upperBoundValue().isEmpty() ) {
							throw QueryLog.INSTANCE.rangePredicateCannotMatchNullValue( getEventContext() );
						}
						RangePredicateBuilder builder = scope().fieldQueryElement( path, PredicateTypeKeys.RANGE );
						builder.within( range, valueModel, valueModel );
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
