/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.FilterPredicateBuilder;
import org.hibernate.search.engine.search.predicate.dsl.FilterPredicateOptionsStep;

public class FilterPredicateOptionsStepImpl<B>
	extends AbstractPredicateFinalStep<B>
	implements FilterPredicateOptionsStep<B> {

	private final SearchPredicateFactory factory;

	private final FilterPredicateBuilder<B> filterPredicateBuilder;

	public FilterPredicateOptionsStepImpl(String name, SearchPredicateBuilderFactory<?, B> builderFactory,
		SearchPredicateFactory factory) {
		super( builderFactory );
		this.factory = factory;
		this.filterPredicateBuilder = builderFactory.def( name );
	}

	@Override
	public <T> FilterPredicateOptionsStep<B> param(String name, T value) {
		filterPredicateBuilder.param( name, value );
		return this;
	}

	@Override
	protected B toImplementation() {
		return filterPredicateBuilder.toImplementation();
	}

}
