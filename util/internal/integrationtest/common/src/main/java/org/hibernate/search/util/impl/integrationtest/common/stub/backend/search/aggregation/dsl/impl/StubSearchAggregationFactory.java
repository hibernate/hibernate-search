/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.spi.AbstractSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public class StubSearchAggregationFactory
		extends
		AbstractSearchAggregationFactory<StubSearchAggregationFactory, SearchAggregationIndexScope<?>, SearchPredicateFactory> {
	public StubSearchAggregationFactory(
			SearchAggregationDslContext<SearchAggregationIndexScope<?>, SearchPredicateFactory> dslContext) {
		super( dslContext );
	}

	@Override
	public StubSearchAggregationFactory withRoot(String objectFieldPath) {
		return new StubSearchAggregationFactory( dslContext.rescope( dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}
}
