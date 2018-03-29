/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.List;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubSearchWorkAssert;

import static org.assertj.core.api.Assertions.assertThat;

class SearchWorkCall<T> {

	private final List<String> indexNames;
	private final StubSearchWork work;
	private final HitAggregator<?, List<T>> hitAggregator;
	private final StubSearchWorkBehavior<?> behavior;

	SearchWorkCall(List<String> indexNames, StubSearchWork work, HitAggregator<?, List<T>> hitAggregator) {
		this.indexNames = indexNames;
		this.work = work;
		this.hitAggregator = hitAggregator;
		this.behavior = null;
	}

	SearchWorkCall(List<String> indexNames, StubSearchWork work, StubSearchWorkBehavior<?> behavior) {
		this.indexNames = indexNames;
		this.work = work;
		this.hitAggregator = null;
		this.behavior = behavior;
	}

	public <U> SearchResult<U> verify(SearchWorkCall<U> actualCall) {
		assertThat( actualCall.indexNames )
				.as( "Search work did not target the expected indexes: " )
				.isEqualTo( indexNames );
		StubSearchWorkAssert.assertThat( actualCall.work )
				.as( "Search work on indexes " + indexNames + " did not match: " )
				.matches( work );
		/*
		 * Checking the result type in the work assertion above guarantees
		 * that the hit collector has the correct type. We can cast safely.
		 */
		long totalHitCount = behavior.getTotalHitCount();
		behavior.contribute( (HitAggregator) actualCall.hitAggregator );
		List<U> hits = actualCall.hitAggregator.build();
		return new SearchResultImpl<>( totalHitCount, hits );
	}

	@Override
	public String toString() {
		return "search work execution on indexes '" + indexNames + "'; work = " + work;
	}

	private static final class SearchResultImpl<T> implements SearchResult<T> {
		private final long totalHitCount;
		private final List<T> hits;

		public SearchResultImpl(long totalHitCount, List<T> hits) {
			this.totalHitCount = totalHitCount;
			this.hits = hits;
		}

		@Override
		public long getHitCount() {
			return totalHitCount;
		}
		@Override
		public List<T> getHits() {
			return hits;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" +
					"totalHitCount=" + totalHitCount +
					", hits=" + hits +
					'}';
		}
	}

}
