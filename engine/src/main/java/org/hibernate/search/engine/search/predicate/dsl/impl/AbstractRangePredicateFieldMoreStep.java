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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.QueryLog;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreGenericStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.reference.predicate.RangePredicateFieldReference;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.reporting.EventContext;

abstract class AbstractRangePredicateFieldMoreStep<
		SR,
		CS extends AbstractRangePredicateFieldMoreStep.GenericCommonState<SR, T, V, S>,
		S extends AbstractRangePredicateFieldMoreStep<SR, CS, S, T, V>,
		T,
		V>
		implements
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	protected final CS commonState;

	private final List<V> fields;
	private final List<RangePredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	public static <SR> RangePredicateFieldMoreStepString<SR> create(
			SearchPredicateDslContext<?> dslContext, String[] fields) {
		return new RangePredicateFieldMoreStepString<>(
				dslContext,
				Arrays.asList( fields )
		);
	}

	public static <SR, T> RangePredicateFieldMoreStepReference<SR, T> create(
			SearchPredicateDslContext<?> dslContext, RangePredicateFieldReference<SR, T>[] fields) {
		return new RangePredicateFieldMoreStepReference<>(
				dslContext,
				Arrays.asList( fields )
		);
	}

	private AbstractRangePredicateFieldMoreStep(CS commonState, List<V> fields) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.fields = fields;
		for ( V field : fields ) {
			// only check that the range predicate can be applied to the requested field:
			commonState.scope().fieldQueryElement( fieldPath( field ), PredicateTypeKeys.RANGE );
		}
	}

	protected abstract String fieldPath(V field);

	protected abstract S thisAsS();

	public S boost(float boost) {
		this.fieldSetBoost = boost;
		return thisAsS();
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( RangePredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	private static class RangePredicateFieldMoreStepString<SR>
			extends
			AbstractRangePredicateFieldMoreStep<SR,
					RangePredicateFieldMoreStepString.CommonState<SR>,
					RangePredicateFieldMoreStepString<SR>,
					Object,
					String>
			implements
			RangePredicateFieldMoreStep<SR,
					RangePredicateFieldMoreStepString<SR>,
					RangePredicateFieldMoreStepString.CommonState<SR>> {

		private RangePredicateFieldMoreStepString(SearchPredicateDslContext<?> dslContext, List<String> fields) {
			this( new CommonState<>( dslContext ), fields );
		}

		private RangePredicateFieldMoreStepString(CommonState<SR> commonState, List<String> fields) {
			super( commonState, fields );
		}

		@Override
		protected String fieldPath(String field) {
			return field;
		}

		@Override
		protected RangePredicateFieldMoreStepString<SR> thisAsS() {
			return this;
		}

		@Override
		public RangePredicateFieldMoreStepString<SR> fields(String... fieldPaths) {
			return new RangePredicateFieldMoreStepString<>( commonState, Arrays.asList( fieldPaths ) );
		}

		@Override
		public CommonState<SR> within(Range<?> range, ValueModel convert) {
			Contracts.assertNotNull( range, "range" );
			Contracts.assertNotNull( convert, "convert" );
			return (CommonState<SR>) commonState.within( range, v -> convert );
		}

		@Override
		public CommonState<SR> withinAny(Collection<? extends Range<?>> ranges, ValueModel valueModel) {
			Contracts.assertNotNull( ranges, "ranges" );
			Contracts.assertNotNull( valueModel, "convert" );
			return (CommonState<SR>) commonState.withinAny( ranges, v -> valueModel );
		}

		public static class CommonState<SR>
				extends GenericCommonState<SR, Object, String, RangePredicateFieldMoreStepString<SR>> {
			CommonState(SearchPredicateDslContext<?> dslContext) {
				super( dslContext );
			}

			@Override
			protected String fieldPath(String field) {
				return field;
			}
		}
	}

	private static class RangePredicateFieldMoreStepReference<SR, T>
			extends
			AbstractRangePredicateFieldMoreStep<SR,
					RangePredicateFieldMoreStepReference.CommonState<SR, T>,
					RangePredicateFieldMoreStepReference<SR, T>,
					T,
					RangePredicateFieldReference<SR, T>>
			implements
			RangePredicateFieldMoreGenericStep<SR,
					RangePredicateFieldMoreStepReference<SR, T>,
					RangePredicateFieldMoreStepReference.CommonState<SR, T>,
					RangePredicateFieldReference<SR, T>,
					T> {

		private RangePredicateFieldMoreStepReference(SearchPredicateDslContext<?> dslContext,
				List<RangePredicateFieldReference<SR, T>> fields) {
			this( new CommonState<>( dslContext ), fields );
		}

		private RangePredicateFieldMoreStepReference(CommonState<SR, T> commonState,
				List<RangePredicateFieldReference<SR, T>> fields) {
			super( commonState, fields );
		}

		@Override
		protected String fieldPath(RangePredicateFieldReference<SR, T> field) {
			return field.absolutePath();
		}

		@Override
		protected RangePredicateFieldMoreStepReference<SR, T> thisAsS() {
			return this;
		}

		@Override
		public RangePredicateFieldMoreStepReference<SR, T> field(RangePredicateFieldReference<SR, T> field) {
			return new RangePredicateFieldMoreStepReference<>( commonState, List.of( field ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public RangePredicateFieldMoreStepReference<SR, T> fields(RangePredicateFieldReference<SR, T>... fields) {
			return new RangePredicateFieldMoreStepReference<>( commonState, Arrays.asList( fields ) );
		}

		@Override
		public CommonState<SR, T> within(Range<? extends T> range) {
			return (CommonState<SR, T>) commonState.within( range, RangePredicateFieldReference::valueModel );
		}

		@Override
		public CommonState<SR, T> withinAny(Collection<? extends Range<?>> ranges, ValueModel valueModel) {
			return (CommonState<SR, T>) commonState.withinAny( ranges, RangePredicateFieldReference::valueModel );
		}


		public static class CommonState<SR, T>
				extends
				GenericCommonState<SR, T, RangePredicateFieldReference<SR, T>, RangePredicateFieldMoreStepReference<SR, T>> {
			CommonState(SearchPredicateDslContext<?> dslContext) {
				super( dslContext );
			}

			@Override
			protected String fieldPath(RangePredicateFieldReference<SR, T> field) {
				return field.absolutePath();
			}
		}
	}

	abstract static class GenericCommonState<SR, T, V, S extends AbstractRangePredicateFieldMoreStep<SR, ?, S, T, V>>
			extends
			AbstractBooleanMultiFieldPredicateCommonState<GenericCommonState<SR, T, V, S>,
					AbstractRangePredicateFieldMoreStep<SR, ?, S, T, V>>
			implements RangePredicateOptionsStep<GenericCommonState<SR, T, V, S>> {

		GenericCommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		@Override
		protected GenericCommonState<SR, T, V, S> thisAsS() {
			return this;
		}

		protected abstract String fieldPath(V field);

		GenericCommonState<SR, T, V, S> within(Range<?> range, Function<V, ValueModel> valueModelFunction) {

			if ( range.lowerBoundValue().isEmpty() && range.upperBoundValue().isEmpty() ) {
				throw QueryLog.INSTANCE.rangePredicateCannotMatchNullValue( getEventContext() );
			}
			for ( var fieldSetState : getFieldSetStates() ) {
				for ( V field : fieldSetState.fields ) {
					ValueModel valueModel = valueModelFunction.apply( field );
					RangePredicateBuilder builder = scope().fieldQueryElement( fieldPath( field ), PredicateTypeKeys.RANGE );
					builder.within( range, valueModel, valueModel );
					fieldSetState.predicateBuilders.add( builder );
				}
			}
			return this;
		}

		public GenericCommonState<SR, T, V, S> withinAny(Collection<? extends Range<?>> ranges,
				Function<V, ValueModel> valueModelFunction) {
			for ( var fieldSetState : getFieldSetStates() ) {
				for ( V field : fieldSetState.fields ) {
					for ( var range : ranges ) {
						Contracts.assertNotNull( range, "range" );
						if ( range.lowerBoundValue().isEmpty() && range.upperBoundValue().isEmpty() ) {
							throw QueryLog.INSTANCE.rangePredicateCannotMatchNullValue( getEventContext() );
						}
						ValueModel valueModel = valueModelFunction.apply( field );
						RangePredicateBuilder builder =
								scope().fieldQueryElement( fieldPath( field ), PredicateTypeKeys.RANGE );
						builder.within( range, valueModel, valueModel );
						fieldSetState.predicateBuilders.add( builder );
					}
				}
			}

			return this;
		}

		protected final EventContext getEventContext() {
			return EventContexts.fromIndexFieldAbsolutePaths(
					getFieldSetStates().stream().flatMap( f -> f.fields.stream() )
							.map( this::fieldPath )
							.collect( Collectors.toList() )
			);
		}
	}

}
