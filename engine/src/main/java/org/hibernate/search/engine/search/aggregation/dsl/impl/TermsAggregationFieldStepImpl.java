/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.impl.Contracts;

public class TermsAggregationFieldStepImpl<PDF extends SearchPredicateFactory> implements TermsAggregationFieldStep<PDF> {
	private final SearchAggregationDslContext<?, PDF> dslContext;

	public TermsAggregationFieldStepImpl(SearchAggregationDslContext<?, PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public <F> TermsAggregationOptionsStep<?, F, Map<F, Long>, PDF> field(String absoluteFieldPath, Class<F> type,
			ValueConvert convert) {
		Contracts.assertNotNull( absoluteFieldPath, "absoluteFieldPath" );
		Contracts.assertNotNull( type, "type" );
		TermsAggregationBuilder<F> builder =
				dslContext.getBuilderFactory().createTermsAggregationBuilder( absoluteFieldPath, type, convert );
		return new TermsAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
