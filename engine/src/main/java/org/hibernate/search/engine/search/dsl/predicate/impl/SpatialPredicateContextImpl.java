/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

class SpatialPredicateContextImpl<B> implements SpatialPredicateContext {

	private final SearchPredicateBuilderFactory<?, B> factory;
	private Float boost;

	SpatialPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public SpatialWithinPredicateContext within() {
		return new SpatialWithinPredicateContextImpl<>( factory, boost );
	}

	@Override
	public SpatialPredicateContext boostedTo(float boost) {
		this.boost = boost;
		return this;
	}
}
