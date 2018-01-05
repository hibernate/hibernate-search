/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.HitCollector;
import org.hibernate.search.util.AssertionFailure;

public final class SimpleHitAggregator<T, U> implements HitAggregator<HitCollector<T>, List<U>> {

	private final Function<T, U> hitTransformer;

	private final HitCollectorImpl hitCollector = new HitCollectorImpl();

	private List<U> hits;

	public SimpleHitAggregator(Function<T, U> hitTransformer) {
		this.hitTransformer = hitTransformer;
	}

	@Override
	public void init(int expectedHitCount) {
		hits = new ArrayList<>( expectedHitCount );
	}

	@Override
	public HitCollector<T> nextCollector() {
		hitCollector.reset();
		return hitCollector;
	}

	@Override
	public List<U> build() {
		List<U> result = hits;
		hits = null;
		return result;
	}

	private class HitCollectorImpl implements HitCollector<T> {

		private boolean currentHitCollected = false;

		@Override
		public void collect(T hit) {
			checkNotAlreadyCollected();
			currentHitCollected = true;
			hits.add( hitTransformer.apply( hit ) );
		}

		public void reset() {
			currentHitCollected = false;
		}

		private void checkNotAlreadyCollected() {
			if ( currentHitCollected ) {
				throw new AssertionFailure( "Received multiple values for a single hit" );
			}
		}

	}
}
