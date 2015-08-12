/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import java.util.concurrent.TimeUnit;

/**
 * @author Emmanuel Bernard
 */
public interface TimeoutManager {
	void start();

	Long getTimeoutLeftInMilliseconds();

	Long getTimeoutLeftInSeconds();

	boolean isTimedOut();

	void stop();

	void setTimeout(long timeout, TimeUnit timeUnit);

	void raiseExceptionOnTimeout();

	void limitFetchingOnTimeout();

	void reactOnQueryTimeoutExceptionWhileExtracting(RuntimeException e);

	boolean hasPartialResults();

	Type getType();

	public static enum Type {
		NONE,
		EXCEPTION,
		LIMIT
	}
}
