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
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilderFactory;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.DelegatingSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.StaticSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;


public class LuceneSearchSortFactoryImpl
		extends DelegatingSearchSortFactory<LuceneSearchPredicateFactory>
		implements LuceneSearchSortFactory {

	private final SearchSortDslContext<LuceneSearchSortBuilderFactory, LuceneSearchSortBuilder, ?> dslContext;

	public LuceneSearchSortFactoryImpl(SearchSortFactory delegate,
			SearchSortDslContext<LuceneSearchSortBuilderFactory, LuceneSearchSortBuilder, LuceneSearchPredicateFactory> dslContext) {
		super( delegate, dslContext );
		this.dslContext = dslContext;
	}

	@Override
	public SortThenStep fromLuceneSortField(SortField luceneSortField) {
		return staticThenStep( dslContext.builderFactory().fromLuceneSortField( luceneSortField ) );
	}

	@Override
	public SortThenStep fromLuceneSort(Sort luceneSort) {
		return staticThenStep( dslContext.builderFactory().fromLuceneSort( luceneSort ) );
	}

	private SortThenStep staticThenStep(LuceneSearchSortBuilder builder) {
		return new StaticSortThenStep<>( dslContext, builder );
	}
}
