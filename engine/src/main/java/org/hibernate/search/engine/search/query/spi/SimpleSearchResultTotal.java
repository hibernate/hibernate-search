/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.spi;

import org.hibernate.search.engine.logging.impl.QueryLog;
import org.hibernate.search.engine.search.query.SearchResultTotal;

public class SimpleSearchResultTotal implements SearchResultTotal {

	public static SimpleSearchResultTotal of(long totalHitCount, boolean isExact) {
		return new SimpleSearchResultTotal( totalHitCount, isExact );
	}

	public static SimpleSearchResultTotal exact(long totalHitCount) {
		return of( totalHitCount, true );
	}

	public static SimpleSearchResultTotal lowerBound(long totalHitCount) {
		return of( totalHitCount, false );
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
			throw QueryLog.INSTANCE.notExactTotalHitCount();
		}
		return totalHitCount;
	}

	@Override
	public long hitCountLowerBound() {
		return totalHitCount;
	}

	@Override
	public String toString() {
		return "SimpleSearchResultTotal{" +
				"totalHitCount=" + totalHitCount +
				", isExact=" + isExact +
				'}';
	}
}
