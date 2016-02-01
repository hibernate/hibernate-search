/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.util.impl;

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
		Object[] numericValues = {
				10,
				20L,
				30.5f,
				40.5d,
				nowCalendar()
		};

		for ( Object val : numericValues ) {
			assertTrue(
					"Value of type " + val.getClass() + " should require numeric range query",
					NumericFieldUtils.requiresNumericRangeQuery( val )
			);
		}
	}

	private Calendar nowCalendar() {
		return GregorianCalendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
	}

	@Test
	public void testShouldNotRequireNumericRangeQuery() {
		assertFalse(
				"null value should not require numeric range query",
				NumericFieldUtils.requiresNumericRangeQuery( null )
		);

		Object[] nonNumericValues = {"", new Date(), BigDecimal.ONE};

		for ( Object val : nonNumericValues ) {
			assertFalse(
					"Value of type '" + val.getClass() + "' should not require numeric range query",
					NumericFieldUtils.requiresNumericRangeQuery( val )
			);
		}
	}

	@Test
	public void testShouldCreateExactMatchQuery() {
		Object[] numericValues = {
				10,
				20L,
				30.5f,
				40.5d,
				nowCalendar(),
				new Date()
		};

		for ( Object val : numericValues ) {
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

		Object[] nonNumericValues = {"", BigDecimal.ONE};

		for ( Object val : nonNumericValues ) {
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
}
