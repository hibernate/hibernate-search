/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public class NonEmptySortContextImpl implements NonEmptySortContext {
	private final SearchSortContainerContext containerContext;
	protected final SearchSortDslContext<?> dslContext;

	public NonEmptySortContextImpl(SearchSortContainerContext containerContext, SearchSortDslContext<?> dslContext) {
		this.containerContext = containerContext;
		this.dslContext = dslContext;
	}

	@Override
	public final SearchSortContainerContext then() {
		return containerContext;
	}

	@Override
	public final SearchSort toSort() {
		return dslContext.toSort();
	}
}
