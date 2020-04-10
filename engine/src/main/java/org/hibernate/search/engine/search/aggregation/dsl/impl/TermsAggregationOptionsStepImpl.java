/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.common.MultiValue;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.impl.Contracts;

class TermsAggregationOptionsStepImpl<F, PDF extends SearchPredicateFactory>
	implements TermsAggregationOptionsStep<TermsAggregationOptionsStepImpl<F, PDF>, F, Map<F, Long>, PDF> {
	private final TermsAggregationBuilder<F> builder;
	private final SearchAggregationDslContext<?, PDF> dslContext;

	TermsAggregationOptionsStepImpl(TermsAggregationBuilder<F> builder, SearchAggregationDslContext<?, PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> orderByCountDescending() {
		builder.orderByCountDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> orderByCountAscending() {
		builder.orderByCountAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> orderByTermAscending() {
		builder.orderByTermAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> orderByTermDescending() {
		builder.orderByTermDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> minDocumentCount(int minDocumentCount) {
		Contracts.assertPositiveOrZero( minDocumentCount, "minDocumentCount" );
		builder.minDocumentCount( minDocumentCount );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> maxTermCount(int maxTermCount) {
		Contracts.assertStrictlyPositive( maxTermCount, "maxTermCount" );
		builder.maxTermCount( maxTermCount );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> mode(MultiValue mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> filter(
		Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.getPredicateFactory() ).toPredicate();

		filter( predicate );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Map<F, Long>> toAggregation() {
		return builder.build();
	}
}
