/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregatonFactory;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public class DefaultSearchAggregationFactory<PDF extends SearchPredicateFactory> implements ExtendedSearchAggregatonFactory<PDF> {

	private final SearchAggregationDslContext<?, PDF> dslContext;

	public DefaultSearchAggregationFactory(SearchAggregationDslContext<?, PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationFieldStep<PDF> range() {
		return new RangeAggregationFieldStepImpl( dslContext );
	}

	@Override
	public TermsAggregationFieldStep<PDF> terms() {
		return new TermsAggregationFieldStepImpl( dslContext );
	}

	@Override
	public <T> T extension(SearchAggregationFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, dslContext )
		);
	}
}
