/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.event.impl;

import java.util.Objects;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;

/**
 * The implementation of EventsIntegratorState used at runtime,
 * after initialization of the ExtendedSearchIntegrator has been
 * performed.
 *
 * @author Sanne Grinovero
 */
final class OptimalEventsIntegratorState implements EventsIntegratorState {

	private final boolean eventsDisabled;
	private final boolean skipDirtyChecks;
	private final ExtendedSearchIntegrator extendedSearchIntegrator;

	public OptimalEventsIntegratorState(boolean eventsDisabled, boolean skipDirtyChecks, ExtendedSearchIntegrator extendedSearchIntegrator) {
		Objects.requireNonNull( extendedSearchIntegrator );
		this.eventsDisabled = eventsDisabled;
		this.skipDirtyChecks = skipDirtyChecks;
		this.extendedSearchIntegrator = extendedSearchIntegrator;
	}

	@Override
	public boolean eventsDisabled() {
		return eventsDisabled;
	}

	@Override
	public ExtendedSearchIntegrator getExtendedSearchIntegrator() {
		return extendedSearchIntegrator;
	}

	@Override
	public boolean skipDirtyChecks() {
		return skipDirtyChecks;
	}

}
