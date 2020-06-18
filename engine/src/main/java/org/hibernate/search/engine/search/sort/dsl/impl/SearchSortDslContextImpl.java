/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

/**
 * A DSL context used when building a {@link SearchSort} object,
 * either when calling {@link MappedIndexScope#sort()} from a search scope
 * or when calling {@link SearchQueryOptionsStep#sort(Function)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchSort} object and cache it).
 */
public final class SearchSortDslContextImpl<F extends SearchSortBuilderFactory<?>, PDF extends SearchPredicateFactory>
		implements SearchSortDslContext<F, PDF> {

	public static <F extends SearchSortBuilderFactory<?>, PDF extends SearchPredicateFactory>
			SearchSortDslContext<F, ?> root(F factory, PDF predicateFactory,
					SearchPredicateBuilderFactory<?> predicateBuilderFactory) {
		return new SearchSortDslContextImpl<>( factory, null, null,
				predicateFactory, predicateBuilderFactory );
	}

	private final F factory;
	private final SearchSortDslContextImpl<F, ?> parent;
	private final SearchSort sort;
	private final PDF predicateFactory;
	private final SearchPredicateBuilderFactory<?> predicateBuilderFactory;

	private SearchSort compositeSort;

	private SearchSortDslContextImpl(F factory, SearchSortDslContextImpl<F, ?> parent, SearchSort sort,
			PDF predicateFactory, SearchPredicateBuilderFactory<?> predicateBuilderFactory) {
		this.factory = factory;
		this.parent = parent;
		this.sort = sort;
		this.predicateFactory = predicateFactory;
		this.predicateBuilderFactory = predicateBuilderFactory;
	}

	@Override
	public F builderFactory() {
		return factory;
	}

	@Override
	public SearchSortDslContext<?, PDF> append(SearchSort sort) {
		return new SearchSortDslContextImpl<>( factory, this, sort,
				predicateFactory, predicateBuilderFactory );
	}

	@Override
	public PDF predicateFactory() {
		return predicateFactory;
	}

	@Override
	public <PDF2 extends SearchPredicateFactory> SearchSortDslContext<F, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension) {
		return new SearchSortDslContextImpl<>(
				factory, parent, sort,
				DslExtensionState.returnIfSupported(
						extension, extension.extendOptional( predicateFactory, predicateBuilderFactory )
				),
				predicateBuilderFactory
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
			return factory.composite().build();
		}
		else if ( parent.sort == null ) {
			// Only one element
			return sort;
		}
		else {
			CompositeSortBuilder builder = factory.composite();
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
