/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

/**
 * Lucene delegates responsibility for efficient time tracking to an external service;
 * this is needed for example by the
 * {@link org.apache.lucene.search.TimeLimitingCollector#TimeLimitingCollector(org.apache.lucene.search.Collector, org.apache.lucene.util.Counter, long)}
 * used by Hibernate Search when time limits are enabled on fulltext queries.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @since 4.1
 */
public interface TimingSource {

	/**
	 * Returns an approximation of {@link java.lang.System#nanoTime()}.
	 * Performance should be preferred over accuracy by the implementation, but the value is monotonic
	 * and expresses time in milliseconds, however, subsequent invocations could return the same value.
	 *
	 * @return an increasing value related to time in milliseconds. Only meaningful to compare time intervals, with no guarantees of high precision.
	 */
	long getMonotonicTimeEstimate();

	/**
	 * Invoked on SearchIntegrator shutdown. There is no start method as it's expected to be lazily initialized
	 */
	void stop();

	/**
	 * Needs to be invoked at least once before {@link #getMonotonicTimeEstimate()} can be used.
	 * Safe to be invoked multiple times.
	 */
	void ensureInitialized();

}
