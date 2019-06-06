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
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilderFactory;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.DelegatingSearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.StaticNonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


public class LuceneSearchSortContainerContextImpl
		extends DelegatingSearchSortContainerContext
		implements LuceneSearchSortContainerContext {

	private final SearchSortDslContext<LuceneSearchSortBuilderFactory, LuceneSearchSortBuilder> dslContext;

	public LuceneSearchSortContainerContextImpl(SearchSortContainerContext delegate,
			SearchSortDslContext<LuceneSearchSortBuilderFactory, LuceneSearchSortBuilder> dslContext) {
		super( delegate );
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext fromLuceneSortField(SortField luceneSortField) {
		return staticNonEmptyContext( dslContext.getFactory().fromLuceneSortField( luceneSortField ) );
	}

	@Override
	public NonEmptySortContext fromLuceneSort(Sort luceneSort) {
		return staticNonEmptyContext( dslContext.getFactory().fromLuceneSort( luceneSort ) );
	}

	private NonEmptySortContext staticNonEmptyContext(LuceneSearchSortBuilder builder) {
		return new StaticNonEmptySortContext<>( dslContext, builder );
	}
}
