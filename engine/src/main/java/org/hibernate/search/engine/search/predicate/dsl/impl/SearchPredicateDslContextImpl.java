/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * A DSL context used when building a {@link SearchPredicate} object,
 * either when calling {@link MappedIndexScope#sort()} from a search scope
 * or when calling {@link SearchQueryOptionsStep#sort(Function)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchPredicate} object and cache it).
 */
public final class SearchPredicateDslContextImpl<F extends SearchPredicateBuilderFactory<?>>
		implements SearchPredicateDslContext<F> {

	public static <F extends SearchPredicateBuilderFactory<?>> SearchPredicateDslContext<F> root(SearchIndexScope<?> scope, F builderFactory) {
		return new SearchPredicateDslContextImpl<>( scope, builderFactory );
	}

	private final SearchIndexScope<?> scope;
	private final F builderFactory;

	private SearchPredicateDslContextImpl(SearchIndexScope<?> scope, F builderFactory) {
		this.scope = scope;
		this.builderFactory = builderFactory;
	}

	@Override
	public SearchIndexScope<?> scope() {
		return scope;
	}

	@Override
	public F builderFactory() {
		return builderFactory;
	}
}
