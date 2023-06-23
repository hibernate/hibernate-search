/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedStep;

public final class SearchPredicateFactoryExtensionStep
		implements SearchPredicateFactoryExtensionIfSupportedStep,
		SearchPredicateFactoryExtensionIfSupportedMoreStep {

	private final SearchPredicateFactory parent;

	private final DslExtensionState<PredicateFinalStep> state = new DslExtensionState<>();

	public SearchPredicateFactoryExtensionStep(SearchPredicateFactory parent) {
		this.parent = parent;
	}

	@Override
	public <T> SearchPredicateFactoryExtensionIfSupportedMoreStep ifSupported(
			SearchPredicateFactoryExtension<T> extension,
			Function<T, ? extends PredicateFinalStep> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent ), predicateContributor );
		return this;
	}

	@Override
	public PredicateFinalStep orElse(
			Function<SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return state.orElse( parent, predicateContributor );
	}

	@Override
	public PredicateFinalStep orElseFail() {
		return state.orElseFail();
	}
}
