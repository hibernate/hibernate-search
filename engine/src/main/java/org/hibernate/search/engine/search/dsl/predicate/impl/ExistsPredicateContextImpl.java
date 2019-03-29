/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.dsl.predicate.ExistsPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.ExistsPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


final class ExistsPredicateContextImpl<B>
		extends AbstractSearchPredicateTerminalContext<B>
		implements ExistsPredicateContext, ExistsPredicateTerminalContext {

	private ExistsPredicateBuilder<B> builder;

	ExistsPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		super( factory );
	}

	@Override
	public ExistsPredicateTerminalContext onField(String absoluteFieldPath) {
		this.builder = factory.exists( absoluteFieldPath );
		return this;
	}

	@Override
	public ExistsPredicateTerminalContext boostedTo(float boost) {
		this.builder.boost( boost );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}
}
