/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortMissingValueBehaviorStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.engine.spatial.GeoPoint;

public class DistanceSortOptionsStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>>
		extends AbstractSortThenStep<SR>
		implements DistanceSortOptionsStep<SR, DistanceSortOptionsStepImpl<SR, PDF>, PDF>,
		DistanceSortMissingValueBehaviorStep<DistanceSortOptionsStepImpl<SR, PDF>> {

	private final DistanceSortBuilder builder;
	private final SearchSortDslContext<SR, ?, ? extends PDF> dslContext;

	public DistanceSortOptionsStepImpl(SearchSortDslContext<SR, ?, ? extends PDF> dslContext,
			String fieldPath, GeoPoint center) {
		super( dslContext );
		this.dslContext = dslContext;
		this.builder = dslContext.scope().fieldQueryElement( fieldPath, SortTypeKeys.DISTANCE );
		builder.center( center );
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> mode(SortMode mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}


	@Override
	public DistanceSortMissingValueBehaviorStep<DistanceSortOptionsStepImpl<SR, PDF>> missing() {
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> first() {
		builder.missingFirst();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> last() {
		builder.missingLast();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> highest() {
		builder.missingHighest();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> lowest() {
		builder.missingLowest();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<SR, PDF> use(GeoPoint value) {
		builder.missingAs( value );
		return this;
	}

	@Override
	protected SearchSort build() {
		return builder.build();
	}

}
