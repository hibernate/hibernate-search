/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * The total for a search result, pertaining to all matched documents,
 * independently from the offset/limit used when fetching hits.
 * <p>
 * The total includes in particular the total hit count,
 * which may be exact (default)
 * or just a lower bound estimate (when particular search options are used).
 */
public interface SearchResultTotal {

	/**
	 * @return Whether the total hit count is exact.
	 * The total hit count is exact by default, but that can change when a
	 * {@link SearchQueryOptionsStep#totalHitCountThreshold(long)} or a
	 * {@link SearchQueryOptionsStep#truncateAfter(long, TimeUnit)} has been defined for the current query.
	 */
	boolean isHitCountExact();

	/**
	 * @return Whether the total hit count is a lower-bound estimate.
	 * The total hit count can be a lower bound only when a {@link SearchQueryOptionsStep#totalHitCountThreshold(long)}
	 * or a {@link SearchQueryOptionsStep#truncateAfter(long, TimeUnit)} has been defined for the current query.
	 */
	boolean isHitCountLowerBound();

	/**
	 * @return The exact value of the total hit count, if available.
	 * Unless you used particular search options,
	 * you can safely assume the exact value of the total hit count is available.
	 * @throws org.hibernate.search.util.common.SearchException if {@link #isHitCountLowerBound()}
	 */
	long hitCount();

	/**
	 * @return A lower-bound estimate of the total hit count.
	 * If the total hit count is known exactly, the exact total hit count is returned.
	 */
	long hitCountLowerBound();

}
