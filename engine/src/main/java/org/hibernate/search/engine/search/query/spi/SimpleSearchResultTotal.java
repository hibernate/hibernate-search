/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class SimpleSearchResultTotal implements SearchResultTotal {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static SimpleSearchResultTotal exact(long totalHitCount) {
		return new SimpleSearchResultTotal( totalHitCount, true );
	}

	public static SimpleSearchResultTotal lowerBound(long totalHitCount) {
		return new SimpleSearchResultTotal( totalHitCount, false );
	}

	private final long totalHitCount;
	private final boolean isExact;

	private SimpleSearchResultTotal(long totalHitCount, boolean isExact) {
		this.totalHitCount = totalHitCount;
		this.isExact = isExact;
	}

	@Override
	public boolean isHitCountExact() {
		return isExact;
	}

	@Override
	public boolean isHitCountLowerBound() {
		return !isExact;
	}

	@Override
	public long hitCount() {
		if ( !isExact ) {
			throw log.notExactTotalHitCount();
		}
		return totalHitCount;
	}

	@Override
	public long hitCountLowerBound() {
		return totalHitCount;
	}
}
