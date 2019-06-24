/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtensionStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


final class SearchPredicateFactoryContextExtensionStepImpl<B> implements SearchPredicateFactoryContextExtensionStep {

	private final SearchPredicateFactoryContext parent;
	private final SearchPredicateBuilderFactory<?, B> factory;

	private final DslExtensionState<PredicateFinalStep> state = new DslExtensionState<>();

	SearchPredicateFactoryContextExtensionStepImpl(SearchPredicateFactoryContext parent,
			SearchPredicateBuilderFactory<?, B> factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchPredicateFactoryContextExtensionStep ifSupported(
			SearchPredicateFactoryContextExtension<T> extension,
			Function<T, ? extends PredicateFinalStep> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), predicateContributor );
		return this;
	}

	@Override
	public PredicateFinalStep orElse(
			Function<SearchPredicateFactoryContext, ? extends PredicateFinalStep> predicateContributor) {
		return state.orElse( parent, predicateContributor );
	}

	@Override
	public PredicateFinalStep orElseFail() {
		return state.orElseFail();
	}
}
