/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.event.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;

/**
 * This EventsIntegratorState is useful to hold for requests
 * onto the actual ExtendedSearchIntegrator until the initialization
 * of the ExtendedSearchIntegrator has been completed.
 *
 * @author Sanne Grinovero
 */
final class InitializingIntegratorState implements EventsIntegratorState {

	private final CompletableFuture<ExtendedSearchIntegrator> extendedIntegratorFuture;

	public InitializingIntegratorState(CompletableFuture<ExtendedSearchIntegrator> extendedIntegratorFuture) {
		this.extendedIntegratorFuture = extendedIntegratorFuture;
	}

	@Override
	public ExtendedSearchIntegrator getExtendedSearchIntegrator() {
		return extendedIntegratorFuture.join();
	}

	@Override
	public boolean eventsDisabled() {
		return FullTextIndexEventListener.eventsDisabled( getExtendedSearchIntegrator() );
	}

	@Override
	public boolean skipDirtyChecks() {
		return FullTextIndexEventListener.skipDirtyChecks( getExtendedSearchIntegrator() );
	}

}
