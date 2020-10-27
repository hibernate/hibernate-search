/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.timing.spi;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class StaticDeadline implements Deadline {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final long remainingTimeMillis;

	StaticDeadline(long remainingTimeMillis) {
		this.remainingTimeMillis = remainingTimeMillis;
	}

	@Override
	public long remainingTimeMillis() {
		return remainingTimeMillis;
	}

	@Override
	public void forceTimeout(Exception cause) {
		throw forceTimeoutAndCreateException( cause );
	}

	@Override
	public SearchTimeoutException forceTimeoutAndCreateException(Exception cause) {
		return log.timedOut( Duration.ofMillis( remainingTimeMillis ), cause );
	}
}
