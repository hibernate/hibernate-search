/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.timeout.impl;

import org.hibernate.search.engine.common.timing.spi.TimingSource;

import org.apache.lucene.util.Counter;

/**
 * Converts our generic TimingSource so that Lucene can use it as a Counter
 *
 * @author Sanne Grinovero
 */
public final class LuceneCounterAdapter extends Counter {

	private final TimingSource timingSource;

	public LuceneCounterAdapter(TimingSource timingSource) {
		timingSource.ensureTimeEstimateIsInitialized();
		this.timingSource = timingSource;
	}

	@Override
	public long addAndGet(final long delta) {
		//parameter delta is ignored as we don't use the clock ticking strategy from Lucene's threads
		//as I don't want to deal with statically referenced threads.
		return timingSource.monotonicTimeEstimate();
	}

	@Override
	public long get() {
		return timingSource.monotonicTimeEstimate();
	}
}
