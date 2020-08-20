/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.event.impl;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;

/**
 * Internal contract to allow switching different strategies
 * to access some configured state from the EventListener, of
 * particular use during (deferred) initialization of the
 * Search engine.
 */
public interface EventsIntegratorState {

	/**
	 * @return true if the Event Listener was disabled
	 */
	boolean eventsDisabled();

	/**
	 * @return the initialized ExtendedSearchIntegrator
	 */
	ExtendedSearchIntegrator getExtendedSearchIntegrator();

	/**
	 * @return true if dirty-checking based optimisations have been disabled
	 */
	boolean skipDirtyChecks();

}
