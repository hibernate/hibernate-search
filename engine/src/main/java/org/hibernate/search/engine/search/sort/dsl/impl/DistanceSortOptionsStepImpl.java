/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class DistanceSortOptionsStepImpl<PDF extends SearchPredicateFactory>
		extends AbstractSortThenStep
		implements DistanceSortOptionsStep<DistanceSortOptionsStepImpl<PDF>, PDF> {

	private final DistanceSortBuilder builder;
	private final SearchSortDslContext<?, ? extends PDF> dslContext;

	public DistanceSortOptionsStepImpl(SearchSortDslContext<?, ? extends PDF> dslContext,
			String absoluteFieldPath, GeoPoint center) {
		super( dslContext );
		this.dslContext = dslContext;
		this.builder = dslContext.builderFactory().distance( absoluteFieldPath );
		builder.center( center );
	}

	@Override
	public DistanceSortOptionsStepImpl<PDF> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<PDF> mode(SortMode mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public DistanceSortOptionsStepImpl<PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	protected SearchSort build() {
		return builder.build();
	}

}
