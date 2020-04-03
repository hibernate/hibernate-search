/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.impl.DefaultSearchSortFactory;

public abstract class AbstractSortThenStep<B> implements SortThenStep {
	private final SearchSortDslContext<?, ? super B, ?> parentDslContext;

	private SearchSortDslContext<?, ? super B, ?> selfDslContext;

	public AbstractSortThenStep(SearchSortDslContext<?, ? super B, ?> parentDslContext) {
		this.parentDslContext = parentDslContext;
	}

	@Override
	public final SearchSortFactory then() {
		return new DefaultSearchSortFactory<>( getSelfDslContext() );
	}

	@Override
	public SearchSort toSort() {
		return getSelfDslContext().toSort();
	}

	protected SearchSortDslContext<?, ? super B, ?> getDslContext() {
		return parentDslContext;
	}

	private SearchSortDslContext<?, ? super B, ?> getSelfDslContext() {
		/*
		 * Postpone the call of toImplementation() as long as possible,
		 * and make sure to only call it once,
		 * so that "finalizing" operations may be performed in toImplementation().
		 * See HSEARCH-3207: we must never call toImplementation() twice, because it may have side-effects.
		 */
		if ( selfDslContext == null ) {
			selfDslContext = parentDslContext.append( toImplementation() );
		}
		return selfDslContext;
	}

	protected abstract B toImplementation();

}
