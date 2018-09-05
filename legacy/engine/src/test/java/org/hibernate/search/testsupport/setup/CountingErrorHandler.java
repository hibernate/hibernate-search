/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.setup;

import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * A test only {@link org.hibernate.search.exception.ErrorHandler} that maintains counts
 * per exception thrown
 *
 * @author gustavonalle
 */
public class CountingErrorHandler implements ErrorHandler {

	private Map<Class<? extends Throwable>, Integer> stats = new HashMap<>();

	@Override
	public void handle(ErrorContext context) {
		register( context.getThrowable() );
	}
	@Override
	public void handleException(String errorMsg, Throwable exception) {
		register( exception );
	}

	public int getCountFor(Class<? extends Throwable> throwable) {
		Integer count = stats.get( throwable );
		return count == null ? 0 : count;
	}

	public int getTotalCount() {
		int total = 0;
		for ( Integer i : stats.values() ) {
			total += i;
		}
		return total;
	}

	private synchronized void register(Throwable exception) {
		Integer count = stats.get( exception.getClass() );
		if ( count == null ) {
			stats.put( exception.getClass(), 1 );
		}
		else {
			stats.put( exception.getClass(), ++ count );
		}
	}
}
