/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import java.util.concurrent.TimeUnit;

/**
 * @author gustavonalle
 */
public class StopTimer {

	private final long start;
	private long elapsed;

	public StopTimer() {
		start = currentTime();
	}

	private long currentTime() {
		return System.currentTimeMillis();
	}

	public void stop() {
		elapsed = currentTime() - start;
	}

	public long getElapsedIn(TimeUnit unit) {
		return unit.convert( elapsed, TimeUnit.MILLISECONDS );
	}
}
