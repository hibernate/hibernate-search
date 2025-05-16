/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

public final class SearchPredicateFactoryExtensionStep<SR>
		implements SearchPredicateFactoryExtensionIfSupportedStep<SR>,
		SearchPredicateFactoryExtensionIfSupportedMoreStep<SR> {

	private final TypedSearchPredicateFactory<SR> parent;

	private final DslExtensionState<PredicateFinalStep> state = new DslExtensionState<>();

	public SearchPredicateFactoryExtensionStep(TypedSearchPredicateFactory<SR> parent) {
		this.parent = parent;
	}

	@Override
	public <T> SearchPredicateFactoryExtensionIfSupportedMoreStep<SR> ifSupported(
			SearchPredicateFactoryExtension<SR, T> extension,
			Function<T, ? extends PredicateFinalStep> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent ), predicateContributor );
		return this;
	}

	@Override
	public PredicateFinalStep orElse(
			Function<TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> predicateContributor) {
		return state.orElse( parent, predicateContributor );
	}

	@Override
	public PredicateFinalStep orElseFail() {
		return state.orElseFail();
	}
}
