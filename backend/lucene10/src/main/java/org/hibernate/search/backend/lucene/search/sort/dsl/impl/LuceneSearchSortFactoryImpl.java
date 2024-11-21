/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

public class LuceneSearchSortFactoryImpl
		extends AbstractSearchSortFactory<
				LuceneSearchSortFactory,
				LuceneSearchSortIndexScope<?>,
				LuceneSearchPredicateFactory>
		implements LuceneSearchSortFactory {

	public LuceneSearchSortFactoryImpl(
			SearchSortDslContext<LuceneSearchSortIndexScope<?>, LuceneSearchPredicateFactory> dslContext) {
		super( dslContext );
	}

	@Override
	public LuceneSearchSortFactory withRoot(String objectFieldPath) {
		return new LuceneSearchSortFactoryImpl( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}

	@Override
	public SortThenStep fromLuceneSortField(SortField luceneSortField) {
		return staticThenStep( dslContext.scope().sortBuilders().fromLuceneSortField( luceneSortField ) );
	}

	@Override
	public SortThenStep fromLuceneSort(Sort luceneSort) {
		return staticThenStep( dslContext.scope().sortBuilders().fromLuceneSort( luceneSort ) );
	}

}
