/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;

public class SearchAggregationDslContextImpl<SC extends SearchAggregationIndexScope, PDF extends SearchPredicateFactory>
		implements SearchAggregationDslContext<SC, PDF> {
	public static <SC extends SearchAggregationIndexScope, PDF extends SearchPredicateFactory>
			SearchAggregationDslContextImpl<SC, PDF> root(SC scope, PDF predicateFactory) {
		return new SearchAggregationDslContextImpl<>( scope, predicateFactory );
	}

	private final SC scope;
	private final PDF predicateFactory;

	private SearchAggregationDslContextImpl(SC scope, PDF predicateFactory) {
		this.scope = scope;
		this.predicateFactory = predicateFactory;
	}

	@Override
	public SC scope() {
		return scope;
	}

	@Override
	public PDF predicateFactory() {
		return predicateFactory;
	}

	@Override
	public <PDF2 extends SearchPredicateFactory> SearchAggregationDslContext<SC, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension) {
		return new SearchAggregationDslContextImpl<>(
				scope,
				predicateFactory.extension( extension )
		);
	}
}
