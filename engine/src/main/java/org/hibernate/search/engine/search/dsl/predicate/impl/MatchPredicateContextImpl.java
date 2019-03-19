/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class MatchPredicateContextImpl<B> implements MatchPredicateContext {

	private final MatchPredicateFieldSetContextImpl.CommonState<B> commonState;

	MatchPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.commonState = new MatchPredicateFieldSetContextImpl.CommonState<>( factory );
	}

	@Override
	public MatchPredicateFieldSetContext onFields(String ... absoluteFieldPaths) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ), DslConverter.ENABLED );
	}

	@Override
	public MatchPredicateFieldSetContext onRawFields(String ... absoluteFieldPaths) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ), DslConverter.DISABLED );
	}
}
