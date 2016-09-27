/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.util;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gregory Fouquet
 */
@TestForIssue(jiraKey = "HSEARCH-2188")
public class NumericFieldUtilsTest {
	@Test
	public void testShouldRequireNumericRangeQuery() {
		for ( Object val : getNumericTestValues() ) {
			assertTrue(
					"Value of type " + val.getClass() + " should require numeric range query",
					NumericFieldUtils.requiresNumericRangeQuery( val )
			);
		}
	}

	@Test
	public void testShouldNotRequireNumericRangeQuery() {
		assertFalse(
				"null value should not require numeric range query",
				NumericFieldUtils.requiresNumericRangeQuery( null )
		);

		for ( Object val : getNonNumericTestValues() ) {
			assertFalse(
					"Value of type '" + val.getClass() + "' should not require numeric range query",
					NumericFieldUtils.requiresNumericRangeQuery( val )
			);
		}
	}

	@Test
	public void testShouldCreateExactMatchQuery() {
		for ( Object val : getNumericTestValues() ) {
			try {
				NumericFieldUtils.createExactMatchQuery( "numField", val );
			}
			catch (SearchException e) {
				fail( "Should create exact match query for value of type " + val.getClass() );
			}
		}
	}

	@Test
	public void testShouldNotCreateExactMatchQuery() {
		SearchException nullEx = null;
		try {
			NumericFieldUtils.createExactMatchQuery( "nonNumField", null );
		}
		catch (SearchException e) {
			nullEx = e;
		}

		assertNotNull( "Should not create exact match query for null value", nullEx );

		for ( Object val : getNonNumericTestValues() ) {
			SearchException caught = null;
			try {
				NumericFieldUtils.createExactMatchQuery( "nonNumField", val );
			}
			catch (SearchException e) {
				caught = e;
			}

			assertNotNull( "Should not create exact match query for value of type " + val.getClass(), caught );
		}
	}

	private Object[] getNumericTestValues() {
		Object[] numericValues = {
				40.5d,
				Byte.valueOf( "100" ),
				Short.valueOf( (short) 4 ),
				20L,
				10,
				30.5f,
				new Date(),
				nowCalendar()
		};
		return numericValues;
	}

	private Object[] getNonNumericTestValues() {
		Object[] nonNumericValues = { "", BigDecimal.ONE };
		return nonNumericValues;
	}

	private Calendar nowCalendar() {
		return GregorianCalendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
	}
}
