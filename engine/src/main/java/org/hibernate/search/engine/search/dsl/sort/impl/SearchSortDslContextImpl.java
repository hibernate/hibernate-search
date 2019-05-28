/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A DSL context used when building a {@link SearchSort} object,
 * either when calling {@link MappedIndexSearchScope#sort()} from a search scope
 * or when calling {@link SearchQueryContext#sort(Consumer)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchSort} object and cache it).
 */
public final class SearchSortDslContextImpl<B>
		implements SearchSortDslContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final SearchSortBuilderFactory<?, B> factory;

	private final List<SearchSortContributor<? extends B>> sortContributors = new ArrayList<>();
	private SearchSort sortResult;
	private boolean usedContributors = false;

	public SearchSortDslContextImpl(SearchSortBuilderFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public void addChild(SearchSortContributor<? extends B> child) {
		if ( usedContributors ) {
			throw log.cannotAddSortToUsedContext();
		}
		sortContributors.add( child );
	}

	@Override
	public SearchSort toSort() {
		if ( sortResult == null ) {
			if ( usedContributors ) {
				// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
				throw new AssertionFailure(
						"A sort object was requested after the corresponding information was contributed to the DSL." +
								" There is a bug in Hibernate Search, please report it."
				);
			}
			List<B> builderResult = getResultingBuilders();
			sortResult = factory.toSearchSort( builderResult );
		}
		return sortResult;
	}

	public List<B> getResultingBuilders() {
		List<B> builderResult = new ArrayList<>();
		if ( sortResult != null ) {
			/*
			 * If the SearchSort object was already created,
			 * we can't use the builders collected by the aggregator anymore: they might be single-use.
			 * We just ask the factory to convert the SearchSort object back to builders.
			 * If the builders can be used multiple times, the factory can optimize this.
			 */
			factory.toImplementation( sortResult, builderResult::add );
		}
		else {
			if ( usedContributors ) {
				// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
				throw new AssertionFailure(
						"A sort contributor was called twice. There is a bug in Hibernate Search, please report it."
				);
			}
			usedContributors = true;
			/*
			 * Optimization: we know the user will not be able to request a SearchSort object anymore,
			 * so we don't need to build a SearchSort object in this case,
			 * we can just use the builders collected by the aggregator directly.
			 */
			for ( SearchSortContributor<? extends B> sortContributor : sortContributors ) {
				sortContributor.contribute( builderResult::add );
			}
		}
		return builderResult;
	}
}
