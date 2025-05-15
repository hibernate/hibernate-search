/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedMoreStep;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.dsl.spi.StaticSortThenStep;

public final class SearchSortFactoryExtensionStep<SR>
		implements SearchSortFactoryExtensionIfSupportedMoreStep<SR> {

	private final TypedSearchSortFactory<SR> parent;
	private final SearchSortDslContext<SR, ?, ?> dslContext;

	private final DslExtensionState<SortFinalStep> state = new DslExtensionState<>();

	public SearchSortFactoryExtensionStep(TypedSearchSortFactory<SR> parent,
			SearchSortDslContext<SR, ?, ?> dslContext) {
		this.parent = parent;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchSortFactoryExtensionIfSupportedMoreStep<SR> ifSupported(
			SearchSortFactoryExtension<T> extension,
			Function<T, ? extends SortFinalStep> sortContributor) {
		state.ifSupported( extension, extension.extendOptional( parent ), sortContributor );
		return this;
	}

	@Override
	public SortThenStep<SR> orElse(Function<TypedSearchSortFactory<SR>, ? extends SortFinalStep> sortContributor) {
		SortFinalStep result = state.orElse( parent, sortContributor );
		return new StaticSortThenStep<>( dslContext, result.toSort() );
	}

	@Override
	public SortThenStep<SR> orElseFail() {
		SortFinalStep result = state.orElseFail();
		return new StaticSortThenStep<>( dslContext, result.toSort() );
	}
}
