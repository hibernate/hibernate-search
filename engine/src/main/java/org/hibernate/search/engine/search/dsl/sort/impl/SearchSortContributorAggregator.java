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

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContributor;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * An aggregator of {@link SearchSortContributor}, ensuring aggregated contributors
 * are used appropriately:
 * <ul>
 *     <li>they must be used at most once</li>
 *     <li>new contributors cannot be added after the other contributors have been used</li>
 * </ul>
 */
public final class SearchSortContributorAggregator<B>
		implements SearchSortContributor<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<SearchSortContributor<? extends B>> sortContributors = new ArrayList<>();

	private boolean contributed = false;

	public void add(SearchSortContributor<? extends B> child) {
		if ( contributed ) {
			throw log.cannotAddSortToUsedContext();
		}
		sortContributors.add( child );
	}

	@Override
	public void contribute(Consumer<? super B> collector) {
		if ( contributed ) {
			// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
			throw new AssertionFailure(
					"A sort contributor was called twice. There is a bug in Hibernate Search, please report it."
			);
		}
		contributed = true;
		for ( SearchSortContributor<? extends B> sortContributor : sortContributors ) {
			sortContributor.contribute( collector );
		}
	}
}
