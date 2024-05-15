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
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsGenericStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

public class FieldSortOptionsGenericStepImpl<SR, T, PDF extends SearchPredicateFactory<SR>>
		extends AbstractSortThenStep<SR>
		implements
		FieldSortOptionsGenericStep<SR,
				T,
				FieldSortOptionsGenericStepImpl<SR, T, PDF>,
				FieldSortOptionsGenericStepImpl<SR, T, PDF>,
				PDF>,
		FieldSortMissingValueBehaviorGenericStep<T, FieldSortOptionsGenericStepImpl<SR, T, PDF>> {

	private final SearchSortDslContext<SR, ?, ? extends PDF> dslContext;
	private final FieldSortBuilder builder;
	private final ValueModel valueModel;

	public FieldSortOptionsGenericStepImpl(SearchSortDslContext<SR, ?, ? extends PDF> dslContext,
			FieldSortFieldReference<? super SR, ?> fieldReference) {
		super( dslContext );
		this.dslContext = dslContext;
		this.builder = dslContext.scope().fieldQueryElement( fieldReference.absolutePath(), SortTypeKeys.FIELD );
		this.valueModel = fieldReference.valueModel();
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> mode(SortMode mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> missing() {
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> first() {
		builder.missingFirst();
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> last() {
		builder.missingLast();
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> highest() {
		builder.missingHighest();
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> lowest() {
		builder.missingLowest();
		return this;
	}

	@Override
	public FieldSortOptionsGenericStepImpl<SR, T, PDF> use(T value) {
		builder.missingAs( value, valueModel );
		return this;
	}

	@Override
	protected SearchSort build() {
		return builder.build();
	}

}
