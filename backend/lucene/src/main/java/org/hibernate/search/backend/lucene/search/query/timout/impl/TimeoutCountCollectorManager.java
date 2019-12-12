/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.timout.impl;

import java.util.Collection;

import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;

import org.apache.lucene.search.CollectorManager;

public class TimeoutCountCollectorManager implements CollectorManager<TimeoutCountCollector, Integer> {

	private final TimeoutManager timeoutManager;

	public TimeoutCountCollectorManager(TimeoutManager timeoutManager) {
		this.timeoutManager = timeoutManager;
	}

	@Override
	public TimeoutCountCollector newCollector() {
		return new TimeoutCountCollector( timeoutManager );
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
