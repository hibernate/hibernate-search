/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinCirclePredicateContext;

public class SpatialWithinCirclePredicateContextImpl<N> implements SpatialWithinCirclePredicateContext<N> {

	private final Supplier<N> nextContextProvider;

	SpatialWithinCirclePredicateContextImpl(Supplier<N> nextContextProvider) {
		this.nextContextProvider = nextContextProvider;
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}
}
