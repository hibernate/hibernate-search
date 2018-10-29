/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.sort.impl;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortContainerContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortFactory;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.DelegatingSearchSortContainerContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.NonEmptySortContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


public class LuceneSearchSortContainerContextImpl<N>
		extends DelegatingSearchSortContainerContextImpl<N>
		implements LuceneSearchSortContainerContext<N> {

	private final LuceneSearchSortFactory factory;

	private final SearchSortDslContext<N, ? super LuceneSearchSortBuilder> dslContext;

	public LuceneSearchSortContainerContextImpl(SearchSortContainerContext<N> delegate,
			LuceneSearchSortFactory factory,
			SearchSortDslContext<N, ? super LuceneSearchSortBuilder> dslContext) {
		super( delegate );
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext<N> fromLuceneSortField(SortField luceneSortField) {
		dslContext.addChild( factory.fromLuceneSortField( luceneSortField ) );
		return nonEmptyContext();
	}

	@Override
	public NonEmptySortContext<N> fromLuceneSort(Sort luceneSort) {
		dslContext.addChild( factory.fromLuceneSort( luceneSort ) );
		return nonEmptyContext();
	}

	private NonEmptySortContext<N> nonEmptyContext() {
		return new NonEmptySortContextImpl<>( this, dslContext );
	}
}
