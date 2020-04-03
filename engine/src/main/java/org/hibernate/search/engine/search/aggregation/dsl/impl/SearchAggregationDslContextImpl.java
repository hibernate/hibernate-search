/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

public class SearchAggregationDslContextImpl<F extends SearchAggregationBuilderFactory<?>>
		implements SearchAggregationDslContext<F> {
	public static <F extends SearchAggregationBuilderFactory<?>> SearchAggregationDslContextImpl root(F builderFactory, SearchPredicateBuilderFactory predicateFactory) {
		return new SearchAggregationDslContextImpl<>( builderFactory, predicateFactory );
	}

	private final F builderFactory;
	private final SearchPredicateBuilderFactory predicateFactory;

	private SearchAggregationDslContextImpl(F builderFactory, SearchPredicateBuilderFactory predicateFactory) {
		this.builderFactory = builderFactory;
		this.predicateFactory = predicateFactory;
	}

	@Override
	public F getBuilderFactory() {
		return builderFactory;
	}

	@Override
	public SearchPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateFactory;
	}
}
