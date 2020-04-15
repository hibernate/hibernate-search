/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.impl.RangeAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.TermsAggregationFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * A delegating {@link SearchAggregationFactory}.
 * <p>
 * Mainly useful when implementing a {@link SearchAggregationFactoryExtension}.
 */
public class DelegatingSearchAggregationFactory<PDF extends SearchPredicateFactory>
		implements ExtendedSearchAggregationFactory<PDF> {

	private final SearchAggregationFactory delegate;
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	public DelegatingSearchAggregationFactory(SearchAggregationFactory delegate,
			SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.delegate = delegate;
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationFieldStep<PDF> range() {
		return new RangeAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public TermsAggregationFieldStep<PDF> terms() {
		return new TermsAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public <T> T extension(SearchAggregationFactoryExtension<T> extension) {
		return delegate.extension( extension );
	}

	protected SearchAggregationFactory getDelegate() {
		return delegate;
	}
}
