/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.reference.sort.FieldSortFieldReference;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortMissingValueBehaviorGenericStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortMissingValueBehaviorStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsGenericStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

public abstract class AbstractFieldSortOptionsGenericStep<
		SR,
		T,
		PDF extends SearchPredicateFactory<SR>,
		S extends AbstractFieldSortOptionsGenericStep<SR, T, PDF, S, N>,
		N extends FieldSortMissingValueBehaviorGenericStep<T, S>>
		extends AbstractSortThenStep<SR>
		implements
		FieldSortOptionsGenericStep<SR,
				T,
				S,
				N,
				PDF> {


	private final SearchSortDslContext<SR, ?, ? extends PDF> dslContext;
	protected final FieldSortBuilder builder;

	public AbstractFieldSortOptionsGenericStep(SearchSortDslContext<SR, ?, ? extends PDF> dslContext,
			String fieldPath) {
		super( dslContext );
		this.dslContext = dslContext;
		this.builder = dslContext.scope().fieldQueryElement( fieldPath, SortTypeKeys.FIELD );
	}

	public static <
			T,
			SR,
			SC extends SearchSortIndexScope<?>,
			PDF extends SearchPredicateFactory<
					SR>> FieldSortOptionsGenericStep<SR, T, ?, ?, ? extends SearchPredicateFactory<SR>> create(
							SearchSortDslContext<SR, SC, PDF> dslContext,
							FieldSortFieldReference<? super SR, T> fieldReference) {
		return new FieldReferenceFieldSortOptionsStep<>( dslContext, fieldReference );
	}

	public static <
			PDF extends SearchPredicateFactory<SR>,
			SR,
			SC extends SearchSortIndexScope<?>> FieldSortOptionsStep<SR, ?, PDF> create(
					SearchSortDslContext<SR, SC, PDF> dslContext, String fieldPath) {
		return new StringFieldSortOptionsStep<>( dslContext, fieldPath );
	}

	@Override
	public S order(SortOrder order) {
		builder.order( order );
		return thisAsS();
	}

	@Override
	public S mode(SortMode mode) {
		builder.mode( mode );
		return thisAsS();
	}

	@Override
	public N missing() {
		return thisAsN();
	}

	@Override
	public S filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public S filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return thisAsS();
	}

	public S first() {
		builder.missingFirst();
		return thisAsS();
	}

	public S last() {
		builder.missingLast();
		return thisAsS();
	}

	public S highest() {
		builder.missingHighest();
		return thisAsS();
	}

	public S lowest() {
		builder.missingLowest();
		return thisAsS();
	}


	@Override
	protected SearchSort build() {
		return builder.build();
	}

	protected abstract S thisAsS();

	protected abstract N thisAsN();


	private static class FieldReferenceFieldSortOptionsStep<SR, T, PDF extends SearchPredicateFactory<SR>>
			extends
			AbstractFieldSortOptionsGenericStep<SR,
					T,
					PDF,
					FieldReferenceFieldSortOptionsStep<SR, T, PDF>,
					FieldReferenceFieldSortOptionsStep<SR, T, PDF>>
			implements FieldSortMissingValueBehaviorGenericStep<T, FieldReferenceFieldSortOptionsStep<SR, T, PDF>> {
		private final ValueModel valueModel;

		public FieldReferenceFieldSortOptionsStep(SearchSortDslContext<SR, ?, ? extends PDF> dslContext,
				FieldSortFieldReference<? super SR, ?> fieldReference) {
			super( dslContext, fieldReference.absolutePath() );
			this.valueModel = fieldReference.valueModel();
		}

		@Override
		public FieldReferenceFieldSortOptionsStep<SR, T, PDF> use(T value) {
			builder.missingAs( value, valueModel );
			return this;
		}

		@Override
		protected FieldReferenceFieldSortOptionsStep<SR, T, PDF> thisAsS() {
			return this;
		}

		@Override
		protected FieldReferenceFieldSortOptionsStep<SR, T, PDF> thisAsN() {
			return this;
		}
	}

	private static class StringFieldSortOptionsStep<SR, PDF extends SearchPredicateFactory<SR>>
			extends
			AbstractFieldSortOptionsGenericStep<SR,
					Object,
					PDF,
					StringFieldSortOptionsStep<SR, PDF>,
					FieldSortMissingValueBehaviorStep<StringFieldSortOptionsStep<SR, PDF>>>
			implements FieldSortOptionsStep<SR, StringFieldSortOptionsStep<SR, PDF>, PDF>,
			FieldSortMissingValueBehaviorStep<StringFieldSortOptionsStep<SR, PDF>> {

		public StringFieldSortOptionsStep(SearchSortDslContext<SR, ?, ? extends PDF> dslContext,
				String fieldPath) {
			super( dslContext, fieldPath );
		}

		@Override
		public StringFieldSortOptionsStep<SR, PDF> use(Object value, ValueModel valueModel) {
			builder.missingAs( value, valueModel );
			return this;
		}

		@Override
		protected StringFieldSortOptionsStep<SR, PDF> thisAsS() {
			return this;
		}

		@Override
		protected StringFieldSortOptionsStep<SR, PDF> thisAsN() {
			return this;
		}
	}
}
