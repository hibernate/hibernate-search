/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import org.hibernate.search.mapper.orm.session.impl.ReadSyncAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.WriteSyncAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.AsyncAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.SyncAutomaticIndexingSynchronizationStrategy;

/**
 * Determines how the thread will block upon committing a transaction
 * where indexed entities were modified.
 *
 * @see SearchSession#setAutomaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy)
 */
public interface AutomaticIndexingSynchronizationStrategy {

	void apply(AutomaticIndexingSynchronizationConfigurationContext context);

	/**
	 * @return A strategy that only waits for index changes to be queued in the backend.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy async() {
		return AsyncAutomaticIndexingSynchronizationStrategy.INSTANCE;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a commit, and waits for the commit to complete.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy writeSync() {
		return WriteSyncAutomaticIndexingSynchronizationStrategy.INSTANCE;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a refresh, and waits for the refresh to complete.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy readSync() {
		return ReadSyncAutomaticIndexingSynchronizationStrategy.INSTANCE;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a commit and a refresh,
	 * and waits for the commit and refresh to complete.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy sync() {
		return SyncAutomaticIndexingSynchronizationStrategy.INSTANCE;
	}

}
