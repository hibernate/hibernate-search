/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class WildcardPredicateContextImpl<B> implements WildcardPredicateContext {

	private final WildcardPredicateFieldSetContextImpl.CommonState<B> commonState;

	WildcardPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.commonState = new WildcardPredicateFieldSetContextImpl.CommonState<>( factory );
	}

	@Override
	public WildcardPredicateContext boostedTo(float boost) {
		commonState.setPredicateLevelBoost( boost );
		return this;
	}

	@Override
	public WildcardPredicateFieldSetContext onFields(String ... absoluteFieldPaths) {
		return new WildcardPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
