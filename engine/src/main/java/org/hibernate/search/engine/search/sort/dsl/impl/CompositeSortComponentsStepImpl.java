/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;

public final class CompositeSortComponentsStepImpl<SR>
		implements CompositeSortComponentsStep<SR, CompositeSortComponentsStepImpl<SR>> {

	private SearchSortDslContext<SR, ?, ?> dslContext;

	public CompositeSortComponentsStepImpl(SearchSortDslContext<SR, ?, ?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CompositeSortComponentsStepImpl<SR> add(SearchSort searchSort) {
		dslContext = dslContext.append( searchSort );
		return this;
	}

	@Override
	public SearchSortFactory<SR> then() {
		return dslContext.then();
	}

	@Override
	public SearchSort toSort() {
		return dslContext.toSort();
	}

}
