/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;


import org.hibernate.CacheMode;

/**
 * @author Yoann Rodiere
 */
public final class MassIndexingJobParameters {

	private MassIndexingJobParameters() {
		// Private constructor, do not use
	}

	public static final String ENTITY_MANAGER_FACTORY_NAMESPACE = "entityManagerFactoryNamespace";

	public static final String ENTITY_MANAGER_FACTORY_REFERENCE = "entityManagerFactoryReference";

	public static final String ENTITY_TYPES = "entityTypes";

	public static final String MAX_THREADS = "maxThreads";

	public static final String MAX_RESULTS_PER_ENTITY = "maxResultsPerEntity";

	public static final String ID_FETCH_SIZE = "idFetchSize";

	public static final String ENTITY_FETCH_SIZE = "entityFetchSize";

	public static final String CACHE_MODE = "cacheMode";

	public static final String OPTIMIZE_ON_FINISH = "optimizeOnFinish";

	public static final String OPTIMIZE_AFTER_PURGE = "optimizeAfterPurge";

	public static final String PURGE_ALL_ON_START = "purgeAllOnStart";

	public static final String ROWS_PER_PARTITION = "rowsPerPartition";

	public static final String CHECKPOINT_INTERVAL = "checkpointInterval";

	public static final String SESSION_CLEAR_INTERVAL = "sessionClearInterval";

	public static final String CUSTOM_QUERY_HQL = "customQueryHQL";

	public static final String CUSTOM_QUERY_CRITERIA = "customQueryCriteria";

	public static final String TENANT_ID = "tenantId";

	public static final class Defaults {

		private Defaults() {
			// Private constructor, do not use
		}

		public static final int ID_FETCH_SIZE = 1_000;
		public static final CacheMode CACHE_MODE = CacheMode.IGNORE;
		public static final boolean OPTIMIZE_ON_FINISH = true;
		public static final boolean OPTIMIZE_AFTER_PURGE = true;
		public static final boolean PURGE_ALL_ON_START = true;

		public static final int ROWS_PER_PARTITION = 20_000;

		public static final int CHECKPOINT_INTERVAL_DEFAULT_RAW = 2_000;
		public static int checkpointInterval(Integer checkpointIntervalRaw, Integer rowsPerPartition) {
			if ( checkpointIntervalRaw != null ) {
				return checkpointIntervalRaw;
			}
			if ( rowsPerPartition == null || rowsPerPartition > CHECKPOINT_INTERVAL_DEFAULT_RAW ) {
				return CHECKPOINT_INTERVAL_DEFAULT_RAW;
			}
			else {
				return rowsPerPartition;
			}
		}

		public static final int SESSION_CLEAR_INTERVAL_DEFAULT_RAW = 200;
		public static int sessionClearInterval(Integer sessionClearIntervalRaw, Integer checkpointInterval) {
			if ( sessionClearIntervalRaw != null ) {
				return sessionClearIntervalRaw;
			}
			if ( checkpointInterval == null || checkpointInterval > SESSION_CLEAR_INTERVAL_DEFAULT_RAW ) {
				return SESSION_CLEAR_INTERVAL_DEFAULT_RAW;
			}
			else {
				return checkpointInterval;
			}
		}

	}
}
