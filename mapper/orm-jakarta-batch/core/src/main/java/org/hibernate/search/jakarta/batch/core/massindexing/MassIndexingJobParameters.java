/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing;

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

	public static final String MERGE_SEGMENTS_ON_FINISH = "mergeSegmentsOnFinish";

	public static final String MERGE_SEGMENTS_AFTER_PURGE = "mergeSegmentsAfterPurge";

	public static final String PURGE_ALL_ON_START = "purgeAllOnStart";

	public static final String ROWS_PER_PARTITION = "rowsPerPartition";

	public static final String CHECKPOINT_INTERVAL = "checkpointInterval";

	public static final String REINDEX_ONLY_HQL = "reindexOnlyHql";
	public static final String REINDEX_ONLY_PARAMETERS = "reindexOnlyParameters";

	public static final String TENANT_ID = "tenantId";

	public static final class Defaults {

		private Defaults() {
			// Private constructor, do not use
		}

		public static final int ID_FETCH_SIZE = 1_000;
		public static final CacheMode CACHE_MODE = CacheMode.IGNORE;
		public static final boolean MERGE_SEGMENTS_ON_FINISH = true;
		public static final boolean MERGE_SEGMENTS_AFTER_PURGE = true;
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

		public static final int ENTITY_FETCH_SIZE_RAW = 200;

		public static int entityFetchSize(Integer entityFetchSizeRaw, Integer checkpointInterval) {
			if ( entityFetchSizeRaw != null ) {
				return entityFetchSizeRaw;
			}
			if ( checkpointInterval == null || checkpointInterval > ENTITY_FETCH_SIZE_RAW ) {
				return ENTITY_FETCH_SIZE_RAW;
			}
			else {
				return checkpointInterval;
			}
		}

	}
}
