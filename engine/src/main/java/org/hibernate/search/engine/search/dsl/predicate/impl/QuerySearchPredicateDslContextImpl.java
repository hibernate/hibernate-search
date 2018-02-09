/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * A DSL context used when calling {@link SearchQueryResultContext#predicate()} to build the predicate
 * in a fluid way (in the same call chain as the query).
 */
public final class QuerySearchPredicateDslContextImpl<N, C>
		implements SearchPredicateDslContext<N, C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final C collector;
	private final Supplier<N> nextContextSupplier;

	private SearchPredicateContributor<C> singlePredicateContributor;

	public QuerySearchPredicateDslContextImpl(C collector, Supplier<N> nextContextSupplier) {
		this.collector = collector;
		this.nextContextSupplier = nextContextSupplier;
	}

	@Override
	public void addContributor(SearchPredicateContributor<C> child) {
		if ( this.singlePredicateContributor != null ) {
			throw log.cannotAddMultiplePredicatesToQueryRoot();
		}
		this.singlePredicateContributor = child;
	}

	@Override
	public N getNextContext() {
		singlePredicateContributor.contribute( collector );
		return nextContextSupplier.get();
	}
}
