/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.jta.timeout;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;

public class TimeoutFailureCollector implements FailureHandler {

	public static final Set<Throwable> EXCEPTIONS = ConcurrentHashMap.newKeySet();

	@Override
	public void handle(FailureContext context) {
		EXCEPTIONS.add( context.throwable() );
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		// do-nothing
	}
}
