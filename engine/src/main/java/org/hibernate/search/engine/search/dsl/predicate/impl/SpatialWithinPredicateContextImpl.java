/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class SpatialWithinPredicateContextImpl<B> implements SpatialWithinPredicateContext {

	private final SpatialWithinPredicateFieldSetContextImpl.CommonState<B> commonState;

	SpatialWithinPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory, Float boost) {
		this.commonState = new SpatialWithinPredicateFieldSetContextImpl.CommonState<>( factory );
		this.commonState.setPredicateLevelBoost( boost );
	}

	@Override
	public SpatialWithinPredicateFieldSetContext onFields(String ... absoluteFieldPaths) {
		return new SpatialWithinPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
