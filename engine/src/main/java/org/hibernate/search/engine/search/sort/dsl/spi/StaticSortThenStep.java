/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import org.hibernate.search.engine.search.sort.SearchSort;

public final class StaticSortThenStep<SR> extends AbstractSortThenStep<SR> {
	final SearchSort sort;

	public StaticSortThenStep(SearchSortDslContext<SR, ?, ?> parentDslContext, SearchSort sort) {
		super( parentDslContext );
		this.sort = sort;
	}

	@Override
	protected SearchSort build() {
		return sort;
	}
}
