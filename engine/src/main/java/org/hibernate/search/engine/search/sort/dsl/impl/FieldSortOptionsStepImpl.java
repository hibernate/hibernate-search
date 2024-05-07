/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortMissingValueBehaviorStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

public class FieldSortOptionsStepImpl<E, PDF extends SearchPredicateFactory<E>>
		extends AbstractSortThenStep<E>
		implements FieldSortOptionsStep<E, FieldSortOptionsStepImpl<E, PDF>, PDF>,
		FieldSortMissingValueBehaviorStep<FieldSortOptionsStepImpl<E, PDF>> {

	private final SearchSortDslContext<E, ?, ? extends PDF> dslContext;
	private final FieldSortBuilder builder;

	public FieldSortOptionsStepImpl(SearchSortDslContext<E, ?, ? extends PDF> dslContext, String fieldPath) {
		super( dslContext );
		this.dslContext = dslContext;
		this.builder = dslContext.scope().fieldQueryElement( fieldPath, SortTypeKeys.FIELD );
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> mode(SortMode mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public FieldSortMissingValueBehaviorStep<FieldSortOptionsStepImpl<E, PDF>> missing() {
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> first() {
		builder.missingFirst();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> last() {
		builder.missingLast();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> highest() {
		builder.missingHighest();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> lowest() {
		builder.missingLowest();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<E, PDF> use(Object value, ValueConvert convert) {
		builder.missingAs( value, convert );
		return this;
	}

	@Override
	protected SearchSort build() {
		return builder.build();
	}

}
