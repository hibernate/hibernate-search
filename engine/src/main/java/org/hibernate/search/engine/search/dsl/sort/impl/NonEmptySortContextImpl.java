/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;

final class NonEmptySortContextImpl<N> implements NonEmptySortContext<N> {
	private final SearchSortContainerContext<N> parent;
	private final SearchSortDslContext<N, ?> dslContext;

	NonEmptySortContextImpl(SearchSortContainerContext<N> parent, SearchSortDslContext<N, ?> dslContext) {
		this.parent = parent;
		this.dslContext = dslContext;
	}

	@Override
	public SearchSortContainerContext<N> then() {
		return parent;
	}

	@Override
	public N end() {
		return dslContext.getNextContext();
	}
}
