/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;

public class NamedPredicateOptionsStepImpl
	extends AbstractPredicateFinalStep
	implements NamedPredicateOptionsStep {

	private final NamedPredicateBuilder builder;

	public NamedPredicateOptionsStepImpl(SearchPredicateFactory predicateFactory, SearchPredicateDslContext<?> dslContext, String name) {
		super( dslContext );
		this.builder = dslContext.builderFactory().named( predicateFactory, name );
	}

	@Override
	public NamedPredicateOptionsStep param(String name, Object value) {
		builder.param( name, value );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}
}