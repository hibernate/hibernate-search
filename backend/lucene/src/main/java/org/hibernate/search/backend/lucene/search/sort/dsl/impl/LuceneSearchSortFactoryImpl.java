/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.dsl.impl;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

public class LuceneSearchSortFactoryImpl<E>
		extends AbstractSearchSortFactory<
				E,
				LuceneSearchSortFactory<E>,
				LuceneSearchSortIndexScope<?>,
				LuceneSearchPredicateFactory<E>>
		implements LuceneSearchSortFactory<E> {

	public LuceneSearchSortFactoryImpl(
			SearchSortDslContext<E, LuceneSearchSortIndexScope<?>, LuceneSearchPredicateFactory<E>> dslContext) {
		super( dslContext );
	}

	@Override
	public LuceneSearchSortFactory<E> withRoot(String objectFieldPath) {
		return new LuceneSearchSortFactoryImpl<>( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}

	@Override
	public SortThenStep<E> fromLuceneSortField(SortField luceneSortField) {
		return staticThenStep( dslContext.scope().sortBuilders().fromLuceneSortField( luceneSortField ) );
	}

	@Override
	public SortThenStep<E> fromLuceneSort(Sort luceneSort) {
		return staticThenStep( dslContext.scope().sortBuilders().fromLuceneSort( luceneSort ) );
	}

}
