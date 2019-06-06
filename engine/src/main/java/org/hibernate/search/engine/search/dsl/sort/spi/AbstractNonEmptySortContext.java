/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.impl.DefaultSearchSortFactoryContext;

public abstract class AbstractNonEmptySortContext<B> implements NonEmptySortContext {
	private final SearchSortDslContext<?, ? super B> parentDslContext;

	private SearchSortDslContext<?, ? super B> selfDslContext;

	public AbstractNonEmptySortContext(SearchSortDslContext<?, ? super B> parentDslContext) {
		this.parentDslContext = parentDslContext;
	}

	@Override
	public final SearchSortFactoryContext then() {
		return new DefaultSearchSortFactoryContext<>( getSelfDslContext() );
	}

	@Override
	public SearchSort toSort() {
		return getSelfDslContext().toSort();
	}

	private SearchSortDslContext<?, ? super B> getSelfDslContext() {
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
