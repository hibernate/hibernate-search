/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import org.hibernate.search.engine.search.sort.SearchSort;

public final class StaticSortThenStep extends AbstractSortThenStep {
	final SearchSort sort;

	public StaticSortThenStep(SearchSortDslContext<?, ?> parentDslContext, SearchSort sort) {
		super( parentDslContext );
		this.sort = sort;
	}

	@Override
	protected SearchSort build() {
		return sort;
	}
}
