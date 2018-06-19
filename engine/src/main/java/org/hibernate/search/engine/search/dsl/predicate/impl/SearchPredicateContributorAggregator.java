/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * An aggregator of {@link SearchPredicateContributor}, ensuring aggregated contributors are used appropriately:
 * <ul>
 *     <li>at most one predicate must be added</li>
 *     <li>the predicate must be used at most once</li>
 *     <li>new contributors cannot be added after the contributor has been used
 *     (the other constraints already prevent that, but we need a specific error message in this case)</li>
 * </ul>
 */
public final class SearchPredicateContributorAggregator<C>
		implements SearchPredicateContributor<C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private SearchPredicateContributor<? super C> singlePredicateContributor;

	private boolean contributed = false;

	public void add(SearchPredicateContributor<? super C> child) {
		if ( contributed ) {
			throw log.cannotAddPredicateToUsedContext();
		}
		if ( this.singlePredicateContributor != null ) {
			throw log.cannotAddMultiplePredicatesToQueryRoot();
		}
		this.singlePredicateContributor = child;
	}

	@Override
	public void contribute(C collector) {
		if ( contributed ) {
			// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
			throw new AssertionFailure(
					"A predicate contributor was called twice. There is a bug in Hibernate Search, please report it."
			);
		}
		contributed = true;
		singlePredicateContributor.contribute( collector );
	}
}
