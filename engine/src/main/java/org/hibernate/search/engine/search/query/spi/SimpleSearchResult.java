/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.List;

public final class SimpleSearchResult<T> implements SearchResult<T> {
	private final long hitCount;
	private final List<T> hits;

	public SimpleSearchResult(long hitCount, List<T> hits) {
		this.hitCount = hitCount;
		this.hits = hits;
	}

	@Override
	public long getHitCount() {
		return hitCount;
	}

	@Override
	public List<T> getHits() {
		return hits;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "hitCount=" + hitCount
				+ ", hits=" + hits
				+ "]";
	}
}
