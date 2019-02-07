/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.dsl.predicate.MatchIdPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class MatchIdPredicateContextImpl<B>
		extends AbstractSearchPredicateTerminalContext<B>
		implements MatchIdPredicateContext {

	private final MatchIdPredicateBuilder<B> matchIdBuilder;

	MatchIdPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		super( factory );
		this.matchIdBuilder = factory.id();
	}

	@Override
	public MatchIdPredicateContext matching(Object value) {
		matchIdBuilder.value( value );
		return this;
	}

	@Override
	protected B toImplementation() {
		return matchIdBuilder.toImplementation();
	}
}
