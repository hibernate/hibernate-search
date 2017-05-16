/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.event.impl;

import java.io.Serializable;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The implementation of EventsIntegratorState which the
 * FullTextIndexEventListener will be pointing to before
 * we start initialization of the SearchIntegrator.
 * This should never be needed, but the parallel initialization
 * of multiple frameworks is tricky to predict:
 * if it happens at least we provide a meaningful error
 * message rather than a null pointer.
 *
 * @author Sanne Grinovero
 */
// Implementation note: do not change this into a singleton.
// The hope is that after boot time there will no longer be any
// instances of this type around, allowing the JVM to avoid
// megamorphic invocations, as these methods are on the hot path.
final class NonInitializedIntegratorState implements EventsIntegratorState, Serializable {

	@Override
	public boolean eventsDisabled() {
		throw notInitialized();
	}

	@Override
	public ExtendedSearchIntegrator getExtendedSearchIntegrator() {
		throw notInitialized();
	}

	@Override
	public boolean skipDirtyChecks() {
		throw notInitialized();
	}

	private SearchException notInitialized() {
		// do not make a static field as we want this class to be disposable.
		// we're unlikely to ever need this logger anyway!
		return LoggerFactory.make().searchIntegratorNotInitialized();
	}

}
