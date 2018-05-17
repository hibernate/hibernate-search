/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;

class SpatialPredicateContextImpl<N, C> implements SpatialPredicateContext<N>, SearchPredicateContributor<C> {

	private final SearchPredicateFactory<C> factory;

	private final Supplier<N> nextContextProvider;

	private SearchPredicateContributor<C> child;

	SpatialPredicateContextImpl(SearchPredicateFactory<C> factory, Supplier<N> nextContextProvider) {
		this.factory = factory;
		this.nextContextProvider = nextContextProvider;
	}

	@Override
	public SpatialWithinPredicateContext<N> within() {
		SpatialWithinPredicateContextImpl<N, C> child = new SpatialWithinPredicateContextImpl<>( factory, nextContextProvider );
		this.child = child;
		return child;
	}

	@Override
	public void contribute(C collector) {
		child.contribute( collector );
	}
}
