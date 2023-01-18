/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.plan.synchronization;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The names of built-in automatic indexing synchronization strategies.
 */
@Incubating
public final class PojoStandaloneIndexingPlanSynchronizationStrategyNames {

	private PojoStandaloneIndexingPlanSynchronizationStrategyNames() {
	}

	/**
	 * A strategy that only waits for index changes to be queued in the backend.
	 * <p>
	 * See the reference documentation for details.
	 */
	public static final String ASYNC = "async";

	/**
	 * A strategy that waits for index changes to be queued and applied, forces a commit, and waits for the commit to complete.
	 * <p>
	 * See the reference documentation for details.
	 */
	public static final String WRITE_SYNC = "write-sync";

	/**
	 * A strategy that waits for index changes to be queued and applied, forces a refresh, and waits for the refresh to complete.
	 * <p>
	 * See the reference documentation for details.
	 */
	public static final String READ_SYNC = "read-sync";

	/**
	 * A strategy that waits for index changes to be queued and applied, forces a commit and a refresh,
	 * and waits for the commit and refresh to complete.
	 * <p>
	 * See the reference documentation for details.
	 */
	public static final String SYNC = "sync";

}
