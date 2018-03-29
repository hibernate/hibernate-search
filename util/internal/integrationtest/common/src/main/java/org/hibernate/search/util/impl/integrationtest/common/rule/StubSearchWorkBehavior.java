/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.query.spi.HitAggregator;

public interface StubSearchWorkBehavior<C> {

	/**
	 * @return The total hit count (which may be larger than the number of hits pushed to the aggregator)
	 */
	long getTotalHitCount();

	/**
	 * @param hitAggregator The hit aggregator to push results to.
	 */
	void contribute(HitAggregator<C, ?> hitAggregator);

	static <C> StubSearchWorkBehavior<C> empty() {
		return of( 0L, c -> { } );
	}

	@SafeVarargs
	static <C> StubSearchWorkBehavior<C> of(long totalHitCount, Consumer<C>... hitContributors) {
		return new StubSearchWorkBehavior<C>() {
			@Override
			public long getTotalHitCount() {
				return totalHitCount;
			}

			@Override
			public void contribute(HitAggregator<C, ?> hitAggregator) {
				hitAggregator.init( hitContributors.length );
				for ( Consumer<C> hitContributor : hitContributors ) {
					C collector = hitAggregator.nextCollector();
					hitContributor.accept( collector );
				}
			}
		};
	}

}
