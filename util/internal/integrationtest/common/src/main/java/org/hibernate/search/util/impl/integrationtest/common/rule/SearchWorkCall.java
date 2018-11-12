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
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubSearchWorkAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.StubHitExtractor;

import static org.assertj.core.api.Assertions.assertThat;

class SearchWorkCall<T> {

	private final List<String> indexNames;
	private final StubSearchWork work;
	private final StubHitExtractor<?, List<T>> hitExtractor;
	private final StubSearchWorkBehavior<?> behavior;

	SearchWorkCall(List<String> indexNames, StubSearchWork work, StubHitExtractor<?, List<T>> hitExtractor) {
		this.indexNames = indexNames;
		this.work = work;
		this.hitExtractor = hitExtractor;
		this.behavior = null;
	}

	SearchWorkCall(List<String> indexNames, StubSearchWork work, StubSearchWorkBehavior<?> behavior) {
		this.indexNames = indexNames;
		this.work = work;
		this.hitExtractor = null;
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
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<U> hits = (List<U>) ((StubHitExtractor) actualCall.hitExtractor).extract( behavior.getRawHits() );
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
