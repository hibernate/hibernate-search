/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * A failure collector with an implicit context.
 * <p>
 * Implementations are thread-safe.
 *
 * @see FailureCollector
 */
public interface ContextualFailureCollector extends FailureCollector, EventContextProvider {

	boolean hasFailure();

	void add(Throwable t);

	void add(String failureMessage);

}
