/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * A DSL context used when building a {@link SearchPredicate} object,
 * either when calling {@link MappedIndexScope#sort()} from a search scope
 * or when calling {@link SearchQueryOptionsStep#sort(Function)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchPredicate} object and cache it).
 */
public final class SearchPredicateDslContextImpl<SC extends SearchPredicateIndexScope>
		implements SearchPredicateDslContext<SC> {

	public static <SC extends SearchPredicateIndexScope> SearchPredicateDslContext<?> root(SC scope) {
		return new SearchPredicateDslContextImpl<>( scope );
	}

	private final SC scope;
	private SearchPredicateDslContextImpl(SC scope) {
		this.scope = scope;
	}

	@Override
	public SC scope() {
		return scope;
	}

}
