/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;

/**
 * A DSL context used when calling {@link SearchQueryContext#sort()} to build the sort
 * in a fluid way (in the same call chain as the query).
 */
public final class FluidSearchSortDslContextImpl<N, B>
		implements SearchSortDslContext<N, B> {

	private final RootSearchSortDslContextImpl<B> rootContext;
	private final N nextContext;

	public FluidSearchSortDslContextImpl(RootSearchSortDslContextImpl<B> rootContext, N nextContext) {
		this.rootContext = rootContext;
		this.nextContext = nextContext;
	}

	@Override
	public void addChild(SearchSortContributor<? extends B> contributor) {
		rootContext.addChild( contributor );
	}

	@Override
	public void addChild(B builder) {
		rootContext.addChild( builder );
	}

	@Override
	public N getNextContext() {
		return nextContext;
	}

}
