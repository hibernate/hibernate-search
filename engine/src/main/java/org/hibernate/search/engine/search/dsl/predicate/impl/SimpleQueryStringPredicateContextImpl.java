/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class SimpleQueryStringPredicateContextImpl<B> implements SimpleQueryStringPredicateContext {

	private final SimpleQueryStringPredicateFieldSetContextImpl.CommonState<B> commonState;

	SimpleQueryStringPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.commonState = new SimpleQueryStringPredicateFieldSetContextImpl.CommonState<>( factory );
	}

	@Override
	public SimpleQueryStringPredicateFieldSetContext onFields(String ... absoluteFieldPaths) {
		return new SimpleQueryStringPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
