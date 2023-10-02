/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;

public final class CompositeSortComponentsStepImpl
		implements CompositeSortComponentsStep<CompositeSortComponentsStepImpl> {

	private SearchSortDslContext<?, ?> dslContext;

	public CompositeSortComponentsStepImpl(SearchSortDslContext<?, ?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CompositeSortComponentsStepImpl add(SearchSort searchSort) {
		dslContext = dslContext.append( searchSort );
		return this;
	}

	@Override
	public SearchSortFactory then() {
		return dslContext.then();
	}

	@Override
	public SearchSort toSort() {
		return dslContext.toSort();
	}

}
