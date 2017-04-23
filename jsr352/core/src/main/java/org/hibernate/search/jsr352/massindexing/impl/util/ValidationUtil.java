/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Utility class for job parameter validation.
 *
 * @author Mincong Huang
 */
public final class ValidationUtil {

	private static final Log log = LoggerFactory.make( Log.class );

	private ValidationUtil() {
		// Private constructor, do not use it.
	}

	public static void validateCheckpointInterval(Integer checkpointInterval, Integer rowsPerPartitions) {
		if ( checkpointInterval != null && rowsPerPartitions != null && checkpointInterval >= rowsPerPartitions ) {
			throw log.illegalCheckpointInterval( checkpointInterval, rowsPerPartitions );
		}
	}

	public static void validatePositive(String parameterName, int parameterValue) {
		if ( parameterValue <= 0 ) {
			throw log.negativeValueOrZero( parameterName, parameterValue );
		}
	}

}
