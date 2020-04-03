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
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.impl.Contracts;

class TermsAggregationOptionsStepImpl<F>
		implements TermsAggregationOptionsStep<TermsAggregationOptionsStepImpl<F>, F, Map<F, Long>> {
	private final TermsAggregationBuilder<F> builder;
	private final SearchAggregationDslContext<?> dslContext;

	TermsAggregationOptionsStepImpl(TermsAggregationBuilder<F> builder, SearchAggregationDslContext<?> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> orderByCountDescending() {
		builder.orderByCountDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> orderByCountAscending() {
		builder.orderByCountAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> orderByTermAscending() {
		builder.orderByTermAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> orderByTermDescending() {
		builder.orderByTermDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> minDocumentCount(int minDocumentCount) {
		Contracts.assertPositiveOrZero( minDocumentCount, "minDocumentCount" );
		builder.minDocumentCount( minDocumentCount );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> maxTermCount(int maxTermCount) {
		Contracts.assertStrictlyPositive( maxTermCount, "maxTermCount" );
		builder.maxTermCount( maxTermCount );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> filter(
		Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {

		SearchAggregationDslContext<?> ctx = dslContext;
		SearchPredicateBuilderFactory predicateBuilderFactory = ctx.getPredicateBuilderFactory();
		SearchPredicateFactory factory = new DefaultSearchPredicateFactory<>( predicateBuilderFactory );
		SearchPredicate predicate = clauseContributor.apply( extendPredicateFactory( factory ) ).toPredicate();

		filter( predicate );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<F> filter(SearchPredicate searchPredicate) {
		SearchAggregationDslContext<?> ctx = dslContext;
		SearchPredicateBuilderFactory predicateBuilderFactory = ctx.getPredicateBuilderFactory();
		searchPredicate = (SearchPredicate) predicateBuilderFactory.toImplementation( searchPredicate );

		builder.filter( searchPredicate );
		return this;
	}

	protected SearchPredicateFactory extendPredicateFactory(SearchPredicateFactory predicateFactory) {
		return predicateFactory;
	}

	@Override
	public SearchAggregation<Map<F, Long>> toAggregation() {
		return builder.build();
	}
}
