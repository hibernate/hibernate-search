/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;

public class StubSearchPredicateFactory<E>
		extends AbstractSearchPredicateFactory<E, StubSearchPredicateFactory<E>, SearchPredicateIndexScope<?>> {
	public StubSearchPredicateFactory(
			SearchPredicateDslContext<SearchPredicateIndexScope<?>> dslContext) {
		super( dslContext );
	}

	@Override
	public StubSearchPredicateFactory<E> withRoot(String objectFieldPath) {
		return new StubSearchPredicateFactory<>( dslContext.rescope( dslContext.scope().withRoot( objectFieldPath ) ) );
	}
}
