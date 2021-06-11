/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

/**
 * A DSL context used when building a {@link SearchSort} object,
 * either when calling {@link MappedIndexScope#sort()} from a search scope
 * or when calling {@link SearchQueryOptionsStep#sort(Function)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchSort} object and cache it).
 */
public final class SearchSortDslContextImpl<F extends SearchSortBuilderFactory, PDF extends SearchPredicateFactory>
		implements SearchSortDslContext<F, PDF> {

	public static <F extends SearchSortBuilderFactory, PDF extends SearchPredicateFactory>
			SearchSortDslContext<F, ?> root(SearchIndexScope<?> scope, F builderFactory, PDF predicateFactory) {
		return new SearchSortDslContextImpl<>( scope, builderFactory, null, null, predicateFactory );
	}

	private final SearchIndexScope<?> scope;
	private final F builderFactory;
	private final SearchSortDslContextImpl<F, ?> parent;
	private final SearchSort sort;
	private final PDF predicateFactory;

	private SearchSort compositeSort;

	private SearchSortDslContextImpl(SearchIndexScope<?> scope,
			F builderFactory, SearchSortDslContextImpl<F, ?> parent, SearchSort sort,
			PDF predicateFactory) {
		this.scope = scope;
		this.builderFactory = builderFactory;
		this.parent = parent;
		this.sort = sort;
		this.predicateFactory = predicateFactory;
	}

	@Override
	public SearchIndexScope<?> scope() {
		return scope;
	}

	@Override
	public F builderFactory() {
		return builderFactory;
	}

	@Override
	public SearchSortDslContext<?, PDF> append(SearchSort sort) {
		return new SearchSortDslContextImpl<>( scope, builderFactory, this, sort, predicateFactory );
	}

	@Override
	public PDF predicateFactory() {
		return predicateFactory;
	}

	@Override
	public <PDF2 extends SearchPredicateFactory> SearchSortDslContext<F, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension) {
		return new SearchSortDslContextImpl<>(
				scope, builderFactory, parent, sort,
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
			return builderFactory.composite().build();
		}
		else if ( parent.sort == null ) {
			// Only one element
			return sort;
		}
		else {
			CompositeSortBuilder builder = builderFactory.composite();
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
