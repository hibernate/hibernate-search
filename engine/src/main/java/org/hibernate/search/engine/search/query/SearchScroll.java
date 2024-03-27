/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query;

/**
 * An ongoing query execution delivering hits continuously from a single snapshot of the index.
 * <p>
 * This is different from classic pagination, where a loop executes the same query multiple times with an incrementing offset.
 * Classic pagination will execute the query against a different snapshot of the index for each call to {@code fetch(...)},
 * potentially leading to some hits appearing in two subsequent pages if the index was modified between two executions.
 * Scrolls do not suffer from this limitation and guarantee that each hit is returned only once.
 * <p>
 * As the scroll maintains a reference to internal resources that ultimately must be freed,
 * the client must call {@link #close()} when it no longer needs the scroll.
 * Additionally, some implementations have an internal timeout beyond which the scroll will automatically close
 * and will no longer be usable.
 *
 * @param <H> The type of hits.
 */
public interface SearchScroll<H> extends AutoCloseable {

	@Override
	void close();

	/**
	 * Returns the next chunk, with at most {@code chunkSize} hits.
	 * <p>
	 * May return a result with less than {@code chunkSize} elements if only that many hits are left.
	 *
	 * @return The next {@link SearchScrollResult}.
	 * @see SearchFetchable#scroll(int)
	 */
	SearchScrollResult<H> next();

}
