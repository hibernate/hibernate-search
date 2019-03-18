/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class PhrasePredicateContextImpl<B> implements PhrasePredicateContext {

	private final PhrasePredicateFieldSetContextImpl.CommonState<B> commonState;

	PhrasePredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.commonState = new PhrasePredicateFieldSetContextImpl.CommonState<>( factory );
	}

	@Override
	public PhrasePredicateFieldSetContext onFields(String ... absoluteFieldPaths) {
		return new PhrasePredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
