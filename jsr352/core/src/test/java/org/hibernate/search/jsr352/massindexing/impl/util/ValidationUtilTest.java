/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import org.hibernate.search.exception.SearchException;

import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class ValidationUtilTest {

	@Test(expected = SearchException.class)
	public void validatePositive_valueIsNegative() throws Exception {
		ValidationUtil.validatePositive( "MyParameter", -1 );
	}

	@Test(expected = SearchException.class)
	public void validatePositive_valueIsZero() throws Exception {
		ValidationUtil.validatePositive( "MyParameter", 0 );
	}

	@Test
	public void validatePositive_valueIsPositive() throws Exception {
		ValidationUtil.validatePositive( "MyParameter", 1 );
		// ok
	}

	@Test
	public void validateCheckpointInterval_checkpointIntervalIsNull() throws Exception {
		ValidationUtil.validateCheckpointInterval( null, 100 );
		// ok
	}

	@Test
	public void validateCheckpointInterval_rowsPerPartitionsIsNull() throws Exception {
		ValidationUtil.validateCheckpointInterval( 100, null );
		// ok
	}

	@Test
	public void validateCheckpointInterval_lessThanRowsPerPartitions() throws Exception {
		ValidationUtil.validateCheckpointInterval( 99, 100 );
		// ok
	}

	@Test(expected = SearchException.class)
	public void validateCheckpointInterval_equalToRowsPerPartitions() throws Exception {
		ValidationUtil.validateCheckpointInterval( 100, 100 );
	}

	@Test(expected = SearchException.class)
	public void validateCheckpointInterval_greaterThanRowsPerPartitions() throws Exception {
		ValidationUtil.validateCheckpointInterval( 101, 100 );
	}

}
