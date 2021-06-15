/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;

/**
 * A DSL context used when building a {@link SearchSort} object,
 * either when calling {@link MappedIndexScope#sort()} from a search scope
 * or when calling {@link SearchQueryOptionsStep#sort(Function)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchSort} object and cache it).
 */
public final class SearchSortDslContextImpl<SC extends SearchSortIndexScope<?>, PDF extends SearchPredicateFactory>
		implements SearchSortDslContext<SC, PDF> {

	public static <SC extends SearchSortIndexScope<?>, PDF extends SearchPredicateFactory>
			SearchSortDslContext<?, ?> root(SC scope, PDF predicateFactory) {
		return new SearchSortDslContextImpl<>( scope, null, null, predicateFactory );
	}

	private final SC scope;
	private final SearchSortDslContextImpl<?, ?> parent;
	private final SearchSort sort;
	private final PDF predicateFactory;

	private SearchSort compositeSort;

	private SearchSortDslContextImpl(SC scope, SearchSortDslContextImpl<?, ?> parent, SearchSort sort,
			PDF predicateFactory) {
		this.scope = scope;
		this.parent = parent;
		this.sort = sort;
		this.predicateFactory = predicateFactory;
	}

	@Override
	public SC scope() {
		return scope;
	}

	@Override
	public SearchSortDslContext<SC, PDF> append(SearchSort sort) {
		return new SearchSortDslContextImpl<>( scope, this, sort, predicateFactory );
	}

	@Override
	public PDF predicateFactory() {
		return predicateFactory;
	}

	@Override
	public <PDF2 extends SearchPredicateFactory> SearchSortDslContext<SC, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension) {
		return new SearchSortDslContextImpl<>(
				scope, parent, sort,
				predicateFactory.extension( extension )
		);
	}

	@Override
	public SearchSort toSort() {
		if ( compositeSort == null ) {
			compositeSort = createCompositeSort();
		}
		return compositeSort;
	}

	private SearchSort createCompositeSort() {
		if ( parent == null ) {
			// No sort at all; just use an empty composite sort.
			return scope.sortBuilders().composite().build();
		}
		else if ( parent.sort == null ) {
			// Only one element
			return sort;
		}
		else {
			CompositeSortBuilder builder = scope.sortBuilders().composite();
			collectSorts( builder );
			return builder.build();
		}
	}

	private void collectSorts(CompositeSortBuilder builder) {
		if ( sort == null ) {
			// We've reached the root
			return;
		}

		parent.collectSorts( builder );
		builder.add( sort );
	}
}
