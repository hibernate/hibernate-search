/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing.session;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;

/**
 * The names of built-in automatic indexing synchronization strategies,
 * accepted by {@link HibernateOrmMapperSettings#AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY}.
 *
 * @deprecated See {@link IndexingPlanSynchronizationStrategyNames}
 */
@Deprecated
public final class AutomaticIndexingSynchronizationStrategyNames {

	private AutomaticIndexingSynchronizationStrategyNames() {
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
