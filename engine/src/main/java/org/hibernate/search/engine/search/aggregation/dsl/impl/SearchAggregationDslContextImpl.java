/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;

public class SearchAggregationDslContextImpl<F extends SearchAggregationBuilderFactory<?>>
		implements SearchAggregationDslContext<F> {
	public static <F extends SearchAggregationBuilderFactory<?>> SearchAggregationDslContextImpl root(F builderFactory) {
		return new SearchAggregationDslContextImpl<>( builderFactory );
	}

	private final F builderFactory;

	private SearchAggregationDslContextImpl(F builderFactory) {
		this.builderFactory = builderFactory;
	}

	@Override
	public F getBuilderFactory() {
		return builderFactory;
	}
}
