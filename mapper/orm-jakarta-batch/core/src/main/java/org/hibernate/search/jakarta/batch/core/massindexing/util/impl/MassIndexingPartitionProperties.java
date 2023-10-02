/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;

/**
 * @author Yoann Rodiere
 */
public final class MassIndexingPartitionProperties {

	private MassIndexingPartitionProperties() {
		// Private constructor, do not use
	}

	public static final String ENTITY_NAME = "entityName";

	public static final String PARTITION_ID = "partitionId";

	public static final String CHECKPOINT_INTERVAL = MassIndexingJobParameters.CHECKPOINT_INTERVAL;

	public static final String LOWER_BOUND = "lowerBound";

	public static final String UPPER_BOUND = "upperBound";

}
