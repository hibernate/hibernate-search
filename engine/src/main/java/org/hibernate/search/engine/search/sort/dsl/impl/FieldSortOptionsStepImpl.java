/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortMissingValueBehaviorStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class FieldSortOptionsStepImpl<B, PDF extends SearchPredicateFactory>
		extends AbstractSortThenStep<B>
		implements FieldSortOptionsStep<FieldSortOptionsStepImpl<B, PDF>, PDF>,
				FieldSortMissingValueBehaviorStep<FieldSortOptionsStepImpl<B, PDF>> {

	private final SearchSortDslContext<?, B, ? extends PDF> dslContext;
	private final FieldSortBuilder<B> builder;

	public FieldSortOptionsStepImpl(SearchSortDslContext<?, B, ? extends PDF> dslContext, String absoluteFieldPath) {
		super( dslContext );
		this.dslContext = dslContext;
		this.builder = dslContext.getBuilderFactory().field( absoluteFieldPath );
	}

	@Override
	public FieldSortOptionsStepImpl<B, PDF> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B, PDF> mode(SortMode mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public FieldSortMissingValueBehaviorStep<FieldSortOptionsStepImpl<B, PDF>> missing() {
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B, PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.getPredicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public FieldSortOptionsStepImpl<B, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B, PDF> first() {
		builder.missingFirst();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B, PDF> last() {
		builder.missingLast();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B, PDF> use(Object value, ValueConvert convert) {
		builder.missingAs( value, convert );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}

}
