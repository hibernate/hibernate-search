/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


class RangePredicateContextImpl<B> implements RangePredicateContext, SearchPredicateContributor<B> {

	private final RangePredicateFieldSetContextImpl.CommonState<B> commonState;

	RangePredicateContextImpl(SearchPredicateFactory<?, B> factory) {
		this.commonState = new RangePredicateFieldSetContextImpl.CommonState<>( factory );
	}

	@Override
	public RangePredicateFieldSetContext onFields(String ... absoluteFieldPaths) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public B contribute() {
		return commonState.contribute();
	}
}
