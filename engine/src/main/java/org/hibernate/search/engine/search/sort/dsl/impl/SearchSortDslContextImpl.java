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

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
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
public final class SearchSortDslContextImpl<F extends SearchSortBuilderFactory<?, B>, B, PDF extends SearchPredicateFactory>
		implements SearchSortDslContext<F, B, PDF> {

	public static <F extends SearchSortBuilderFactory<?, B>, B, PDF extends SearchPredicateFactory>
			SearchSortDslContext<F, B, ?> root(F factory, PDF predicateFactory,
					SearchPredicateBuilderFactory<?, ?> predicateBuilderFactory) {
		return new SearchSortDslContextImpl<>( factory, null, null,
				predicateFactory, predicateBuilderFactory );
	}

	private final F factory;
	private final SearchSortDslContextImpl<F, B, ?> parent;
	private final B builder;
	private final PDF predicateFactory;
	private final SearchPredicateBuilderFactory<?, ?> predicateBuilderFactory;

	private SearchSort sortResult;

	private SearchSortDslContextImpl(F factory, SearchSortDslContextImpl<F, B, ?> parent, B builder,
			PDF predicateFactory, SearchPredicateBuilderFactory<?, ?> predicateBuilderFactory) {
		this.factory = factory;
		this.parent = parent;
		this.builder = builder;
		this.predicateFactory = predicateFactory;
		this.predicateBuilderFactory = predicateBuilderFactory;
	}

	@Override
	public F getBuilderFactory() {
		return factory;
	}

	@Override
	public SearchSortDslContext<?, B, PDF> append(B builder) {
		return new SearchSortDslContextImpl<>( factory, this, builder,
				predicateFactory, predicateBuilderFactory );
	}

	@Override
	public PDF getPredicateFactory() {
		return predicateFactory;
	}

	@Override
	public <PDF2 extends SearchPredicateFactory> SearchSortDslContext<F, B, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension) {
		return new SearchSortDslContextImpl<>(
				factory, parent, builder,
				DslExtensionState.returnIfSupported(
						extension, extension.extendOptional( predicateFactory, predicateBuilderFactory )
				),
				predicateBuilderFactory
		);
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
