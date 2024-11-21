/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.hibernate.search.engine.common.timing.Deadline;

import org.apache.lucene.search.CollectorManager;

public class TimeoutCountCollectorManager implements CollectorManager<TimeoutCountCollector, Integer> {

	private final Deadline deadline;

	public TimeoutCountCollectorManager(Deadline deadline) {
		this.deadline = deadline;
	}

	@Override
	public TimeoutCountCollector newCollector() {
		return new TimeoutCountCollector( deadline );
	}

	@Override
	public Integer reduce(Collection<TimeoutCountCollector> collectors) {
		int total = 0;
		for ( TimeoutCountCollector collector : collectors ) {
			total += collector.getTotalHits();
		}
		return total;
	}
}
