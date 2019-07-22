/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.aggregation.impl;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.dsl.aggregation.TermsAggregationOptionsStep;
import org.hibernate.search.util.common.impl.Contracts;

class TermsAggregationOptionsStepImpl<F> implements TermsAggregationOptionsStep<F, Map<F, Long>> {
	private final TermsAggregationBuilder<F> builder;

	TermsAggregationOptionsStepImpl(TermsAggregationBuilder<F> builder) {
		this.builder = builder;
	}

	@Override
	public TermsAggregationOptionsStep<F, Map<F, Long>> orderByCountDescending() {
		builder.orderByCountDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStep<F, Map<F, Long>> orderByCountAscending() {
		builder.orderByCountAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStep<F, Map<F, Long>> orderByTermAscending() {
		builder.orderByTermAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStep<F, Map<F, Long>> orderByTermDescending() {
		builder.orderByTermDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStep<F, Map<F, Long>> minDocumentCount(int minDocumentCount) {
		Contracts.assertPositiveOrZero( minDocumentCount, "minDocumentCount" );
		builder.minDocumentCount( minDocumentCount );
		return this;
	}

	@Override
	public TermsAggregationOptionsStep<F, Map<F, Long>> maxTermCount(int maxTermCount) {
		Contracts.assertStrictlyPositive( maxTermCount, "maxTermCount" );
		builder.maxTermCount( maxTermCount );
		return this;
	}

	@Override
	public SearchAggregation<Map<F, Long>> toAggregation() {
		return builder.build();
	}
}
