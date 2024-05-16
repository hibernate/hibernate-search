/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;

public abstract class AbstractSortThenStep<E> implements SortThenStep<E> {
	private final SearchSortDslContext<E, ?, ?> parentDslContext;

	private SearchSortDslContext<E, ?, ?> selfDslContext;

	public AbstractSortThenStep(SearchSortDslContext<E, ?, ?> parentDslContext) {
		this.parentDslContext = parentDslContext;
	}

	@Override
	public final SearchSortFactory<E> then() {
		return selfDslContext().then();
	}

	@Override
	public SearchSort toSort() {
		return selfDslContext().toSort();
	}

	private SearchSortDslContext<E, ?, ?> selfDslContext() {
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
