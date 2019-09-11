/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;

/**
 * A delegating {@link SearchAggregationFactory}.
 * <p>
 * Mainly useful when implementing a {@link SearchAggregationFactoryExtension}.
 */
public class DelegatingSearchAggregationFactory implements SearchAggregationFactory {

	private final SearchAggregationFactory delegate;

	public DelegatingSearchAggregationFactory(SearchAggregationFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public RangeAggregationFieldStep range() {
		return delegate.range();
	}

	@Override
	public TermsAggregationFieldStep terms() {
		return delegate.terms();
	}

	@Override
	public <T> T extension(SearchAggregationFactoryExtension<T> extension) {
		return delegate.extension( extension );
	}

	protected SearchAggregationFactory getDelegate() {
		return delegate;
	}
}
