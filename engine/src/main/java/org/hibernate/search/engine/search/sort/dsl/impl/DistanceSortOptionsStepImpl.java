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
import org.hibernate.search.engine.search.sort.dsl.DistanceSortMissingValueBehaviorStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.engine.spatial.GeoPoint;

public class DistanceSortOptionsStepImpl<E, PDF extends SearchPredicateFactory<E>>
		extends AbstractSortThenStep<E>
		implements DistanceSortOptionsStep<E, DistanceSortOptionsStepImpl<E, PDF>, PDF>,
		DistanceSortMissingValueBehaviorStep<DistanceSortOptionsStepImpl<E, PDF>> {

	private final DistanceSortBuilder builder;
	private final SearchSortDslContext<E, ?, ? extends PDF> dslContext;

	public DistanceSortOptionsStepImpl(SearchSortDslContext<E, ?, ? extends PDF> dslContext,
			String fieldPath, GeoPoint center) {
		super( dslContext );
		this.dslContext = dslContext;
		this.builder = dslContext.scope().fieldQueryElement( fieldPath, SortTypeKeys.DISTANCE );
		builder.center( center );
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> mode(SortMode mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}


	@Override
	public DistanceSortMissingValueBehaviorStep<DistanceSortOptionsStepImpl<E, PDF>> missing() {
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> first() {
		builder.missingFirst();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> last() {
		builder.missingLast();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> highest() {
		builder.missingHighest();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> lowest() {
		builder.missingLowest();
		return this;
	}

	@Override
	public DistanceSortOptionsStepImpl<E, PDF> use(GeoPoint value) {
		builder.missingAs( value );
		return this;
	}

	@Override
	protected SearchSort build() {
		return builder.build();
	}

}
