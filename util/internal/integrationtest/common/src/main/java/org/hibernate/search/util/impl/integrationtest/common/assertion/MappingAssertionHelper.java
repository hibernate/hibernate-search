/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;

import org.awaitility.Awaitility;

public abstract class MappingAssertionHelper<E> {

	private final boolean supportsExplicitRefresh;

	protected MappingAssertionHelper(BackendConfiguration backendConfiguration) {
		this.supportsExplicitRefresh = backendConfiguration.supportsExplicitRefresh();
	}

	protected MappingAssertionHelper(BackendSetupStrategy backendSetupStrategy) {
		this.supportsExplicitRefresh = backendSetupStrategy.supportsExplicitRefresh();
	}

	public void searchAfterIndexChanges(E entryPoint, Runnable assertion) {
		if ( supportsExplicitRefresh ) {
			doRefresh( entryPoint );
		}
		searchAfterIndexChangesAndPotentialRefresh( assertion );
	}

	protected abstract void doRefresh(E entryPoint);

	public void searchAfterIndexChangesAndPotentialRefresh(Runnable assertion) {
		if ( supportsExplicitRefresh ) {
			// The refresh actually occurred: we can run the assertion now.
			assertion.run();
		}
		else {
			// The refresh did not actually occur: we cannot expect the assertion to succeed immediately.
			// This will lead to potentially long (~1s) waits,
			// but we don't have a choice since we can't do an explicit refresh.
			// Also, this will only work for assertions that don't pass before changes,
			// but do pass afterward.
			// Things like "checking we still have the same state" may produce false positives.
			Awaitility.await().untilAsserted( assertion::run );
		}
	}


}
