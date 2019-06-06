/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.CompositeSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;

class CompositeSortContextImpl<B> implements CompositeSortContext {

	private SearchSortDslContext<?, B> dslContext;

	CompositeSortContextImpl(SearchSortDslContext<?, B> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CompositeSortContext add(SearchSort searchSort) {
		dslContext = dslContext.append( dslContext.getFactory().toImplementation( searchSort ) );
		return this;
	}

	@Override
	public SearchSortFactoryContext then() {
		return new DefaultSearchSortFactoryContext<>( dslContext );
	}

	@Override
	public SearchSort toSort() {
		return dslContext.toSort();
	}

}
