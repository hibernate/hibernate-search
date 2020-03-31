/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

/**
 * A DSL context used when building a {@link SearchSort} object,
 * either when calling {@link MappedIndexScope#sort()} from a search scope
 * or when calling {@link SearchQueryOptionsStep#sort(Function)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchSort} object and cache it).
 */
public final class SearchSortDslContextImpl<F extends SearchSortBuilderFactory<?, B>, B>
		implements SearchSortDslContext<F, B> {

	public static <F extends SearchSortBuilderFactory<?, B>, B> SearchSortDslContext<F, B> root(F factory, SearchPredicateBuilderFactory predicateFactory) {
		return new SearchSortDslContextImpl<>( factory, null, null, predicateFactory );
	}

	private final F factory;
	private final SearchPredicateBuilderFactory predicateBuilderFactory;
	private final SearchSortDslContextImpl<F, B> parent;
	private final B builder;

	private SearchSort sortResult;

	private SearchSortDslContextImpl(F factory, SearchSortDslContextImpl<F, B> parent, B builder, SearchPredicateBuilderFactory predicateFactory) {
		this.factory = factory;
		this.parent = parent;
		this.builder = builder;
		this.predicateBuilderFactory = predicateFactory;
	}

	@Override
	public F getBuilderFactory() {
		return factory;
	}

	@Override
	public SearchPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	@Override
	public SearchSortDslContext<?, B> append(B builder) {
		return new SearchSortDslContextImpl<>( factory, this, builder, predicateBuilderFactory );
	}

	@Override
	public SearchSort toSort() {
		if ( sortResult == null ) {
			List<B> builders = new ArrayList<>();
			collectBuilders( builders );
			sortResult = factory.toSearchSort( builders );
		}
		return sortResult;
	}

	private void collectBuilders(List<B> builders) {
		if ( builder != null ) { // Otherwise we've reached the root
			parent.collectBuilders( builders );
			builders.add( builder );
		}
	}
}
