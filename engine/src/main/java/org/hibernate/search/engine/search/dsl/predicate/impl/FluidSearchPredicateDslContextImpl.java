/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;

/**
 * A DSL context used when calling {@link SearchQueryResultContext#predicate()} to build the predicate
 * in a fluid way (in the same call chain as the query).
 */
public final class FluidSearchPredicateDslContextImpl<N, B>
		implements SearchPredicateDslContext<N, B> {

	private final RootSearchPredicateDslContextImpl<B> rootDslContext;

	private final Supplier<N> nextContextSupplier;

	public FluidSearchPredicateDslContextImpl(RootSearchPredicateDslContextImpl<B> rootDslContext,
			Supplier<N> nextContextSupplier) {
		this.rootDslContext = rootDslContext;
		this.nextContextSupplier = nextContextSupplier;
	}

	@Override
	public void addChild(SearchPredicateContributor<? extends B> contributor) {
		rootDslContext.addChild( contributor );
	}

	@Override
	public void addChild(B builder) {
		rootDslContext.addChild( builder );
	}

	@Override
	public N getNextContext() {
		return nextContextSupplier.get();
	}

}
