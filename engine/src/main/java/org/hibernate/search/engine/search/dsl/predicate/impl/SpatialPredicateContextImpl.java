/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;

class SpatialPredicateContextImpl<B> implements SpatialPredicateContext, SearchPredicateContributor<B> {

	private final SearchPredicateFactory<?, B> factory;


	private SearchPredicateContributor<B> child;

	SpatialPredicateContextImpl(SearchPredicateFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public SpatialWithinPredicateContext within() {
		SpatialWithinPredicateContextImpl<B> child = new SpatialWithinPredicateContextImpl<>( factory );
		this.child = child;
		return child;
	}

	@Override
	public B contribute() {
		return child.contribute();
	}

}
