/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import org.hibernate.search.mapper.orm.session.impl.CommittedAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.QueuedAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.SearchableAutomaticIndexingSynchronizationStrategy;

/**
 * Determines how the thread will block upon committing a transaction
 * where indexed entities were modified.
 *
 * @see SearchSession#setAutomaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy)
 */
public interface AutomaticIndexingSynchronizationStrategy {

	void apply(AutomaticIndexingSynchronizationConfigurationContext context);

	/**
	 * @return A strategy that only waits for indexing requests to be queued in the backend.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy queued() {
		return QueuedAutomaticIndexingSynchronizationStrategy.INSTANCE;
	}

	/**
	 * @return A strategy that waits for indexing requests to be committed.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy committed() {
		return CommittedAutomaticIndexingSynchronizationStrategy.INSTANCE;
	}

	/**
	 * @return A strategy that waits for indexing requests to be committed and forces index refreshes.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy searchable() {
		return SearchableAutomaticIndexingSynchronizationStrategy.INSTANCE;
	}

}
