/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import java.util.function.Function;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFilterStep;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregatonFactory;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.impl.RangeAggregationFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.aggregation.dsl.impl.TermsAggregationFieldStepImpl;

/**
 * A delegating {@link SearchAggregationFactory}.
 * <p>
 * Mainly useful when implementing a {@link SearchAggregationFactoryExtension}.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
public class DelegatingSearchAggregationFactory<PDF extends SearchPredicateFactory> implements ExtendedSearchAggregatonFactory<PDF> {

	private final SearchAggregationFactory delegate;
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	public DelegatingSearchAggregationFactory(SearchAggregationFactory delegate,
			SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.delegate = delegate;
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
		return delegate.extension( extension );
	}

	protected SearchAggregationFactory getDelegate() {
		return delegate;
	}
}
