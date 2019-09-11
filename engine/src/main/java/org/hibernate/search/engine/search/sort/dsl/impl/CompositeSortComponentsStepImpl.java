/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;

class CompositeSortComponentsStepImpl<B> implements CompositeSortComponentsStep {

	private SearchSortDslContext<?, B> dslContext;

	CompositeSortComponentsStepImpl(SearchSortDslContext<?, B> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CompositeSortComponentsStep add(SearchSort searchSort) {
		dslContext = dslContext.append( dslContext.getBuilderFactory().toImplementation( searchSort ) );
		return this;
	}

	@Override
	public SearchSortFactory then() {
		return new DefaultSearchSortFactory<>( dslContext );
	}

	@Override
	public SearchSort toSort() {
		return dslContext.toSort();
	}

}
