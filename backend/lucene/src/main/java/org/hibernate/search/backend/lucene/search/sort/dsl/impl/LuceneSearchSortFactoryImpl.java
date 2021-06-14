/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.dsl.impl;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.DelegatingSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.StaticSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;


public class LuceneSearchSortFactoryImpl
		extends DelegatingSearchSortFactory<LuceneSearchSortFactory, LuceneSearchPredicateFactory>
		implements LuceneSearchSortFactory {

	private final SearchSortDslContext<LuceneSearchSortIndexScope, LuceneSearchPredicateFactory> dslContext;

	public LuceneSearchSortFactoryImpl(SearchSortFactory delegate,
			SearchSortDslContext<LuceneSearchSortIndexScope, LuceneSearchPredicateFactory> dslContext) {
		super( delegate, dslContext );
		this.dslContext = dslContext;
	}

	@Override
	public SortThenStep fromLuceneSortField(SortField luceneSortField) {
		return staticThenStep( dslContext.scope().sortBuilders().fromLuceneSortField( luceneSortField ) );
	}

	@Override
	public SortThenStep fromLuceneSort(Sort luceneSort) {
		return staticThenStep( dslContext.scope().sortBuilders().fromLuceneSort( luceneSort ) );
	}

	private SortThenStep staticThenStep(SearchSort sort) {
		return new StaticSortThenStep( dslContext, sort );
	}
}
