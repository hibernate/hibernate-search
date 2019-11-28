/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.timeout.impl;

import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;

import org.apache.lucene.util.Counter;

/**
 * Converts our generic TimingSource so that Lucene can use it as a Counter
 *
 * @author Sanne Grinovero
 */
public final class LuceneCounterAdapter extends Counter {

	private final TimingSource timingSource;

	public LuceneCounterAdapter(TimingSource timingSource) {
		timingSource.ensureInitialized();
		this.timingSource = timingSource;
	}

	@Override
	public long addAndGet(final long delta) {
		//parameter delta is ignored as we don't use the clock ticking strategy from Lucene's threads
		//as I don't want to deal with statically referenced threads.
		return timingSource.getMonotonicTimeEstimate();
	}

	@Override
	public long get() {
		return timingSource.getMonotonicTimeEstimate();
	}
}
