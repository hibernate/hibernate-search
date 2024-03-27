/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;

public abstract class AbstractSortThenStep implements SortThenStep {
	private final SearchSortDslContext<?, ?> parentDslContext;

	private SearchSortDslContext<?, ?> selfDslContext;

	public AbstractSortThenStep(SearchSortDslContext<?, ?> parentDslContext) {
		this.parentDslContext = parentDslContext;
	}

	@Override
	public final SearchSortFactory then() {
		return selfDslContext().then();
	}

	@Override
	public SearchSort toSort() {
		return selfDslContext().toSort();
	}

	private SearchSortDslContext<?, ?> selfDslContext() {
		/*
		 * Postpone the call of build() as long as possible,
		 * and make sure to only call it once,
		 * so that "finalizing" operations may be performed in build().
		 * See HSEARCH-3207: we must never call build() twice, because it may have side-effects.
		 */
		if ( selfDslContext == null ) {
			selfDslContext = parentDslContext.append( build() );
		}
		return selfDslContext;
	}

	protected abstract SearchSort build();

}
