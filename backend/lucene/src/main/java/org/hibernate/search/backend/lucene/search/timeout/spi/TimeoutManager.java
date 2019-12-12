/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.timeout.spi;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author Emmanuel Bernard
 */
public interface TimeoutManager {

	void start();

	Long getTimeoutLeftInMilliseconds();

	boolean isTimedOut();

	void stop();

	void setTimeout(long timeout, TimeUnit timeUnit);

	void forceTimedOut();

	void raiseExceptionOnTimeout();

	void limitFetchingOnTimeout();

	Type getType();

	Duration getTookTime();

	enum Type {
		NONE,
		EXCEPTION,
		LIMIT
	}
}
