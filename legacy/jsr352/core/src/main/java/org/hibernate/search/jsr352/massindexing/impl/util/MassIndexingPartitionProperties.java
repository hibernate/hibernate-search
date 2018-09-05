/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;


import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;

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

	public static final String INDEX_SCOPE = "indexScope";
}
