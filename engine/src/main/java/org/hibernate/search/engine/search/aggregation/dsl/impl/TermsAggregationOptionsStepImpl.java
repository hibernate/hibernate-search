/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationOptionsStep;
import org.hibernate.search.engine.search.common.MultiValue;
import org.hibernate.search.util.common.impl.Contracts;

class TermsAggregationOptionsStepImpl<F>
		implements TermsAggregationOptionsStep<TermsAggregationOptionsStepImpl<F>, F, Map<F, Long>> {
	private final TermsAggregationBuilder<F> builder;

	TermsAggregationOptionsStepImpl(TermsAggregationBuilder<F> builder) {
		this.builder = builder;
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
	public TermsAggregationOptionsStepImpl<F> multi(MultiValue multi) {
		builder.multi( multi );
		return this;
	}

	@Override
	public SearchAggregation<Map<F, Long>> toAggregation() {
		return builder.build();
	}
}
