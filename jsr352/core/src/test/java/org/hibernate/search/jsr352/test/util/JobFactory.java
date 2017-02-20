/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.test.util;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

/**
 * @author Mincong Huang
 */
public final class JobFactory {

	private static final JobOperator OPERATOR = BatchRuntime.getJobOperator();

	public static JobOperator getJobOperator() {
		return OPERATOR;
	}

	private JobFactory() {
		// Private constructor, don't use it
	}
}
