/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.builtin.StringEncodingDateBridge;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class StringEncodingDateBridgeTest {

	private static final TimeZone ENCODING_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	private StringEncodingDateBridge bridgeUnderTest;
	private Date testDate;
	private Document testDocument;

	@Before
	public void setUp() {
		bridgeUnderTest = new StringEncodingDateBridge( Resolution.MILLISECOND );

		Calendar calendar = GregorianCalendar.getInstance( ENCODING_TIME_ZONE, Locale.ROOT );
		testDate = calendar.getTime();

		testDocument = new Document();
		StringField stringDateField = new StringField(
				"date", DateTools.dateToString( testDate, DateTools.Resolution.MILLISECOND ), Field.Store.NO
		);
		testDocument.add( stringDateField );


		StringField invalidDateField = new StringField(
				"invalidDate", "foo", Field.Store.NO
		);
		testDocument.add( invalidDateField );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1752")
	public void testFieldBridgeGetReturnsDateInstance() {
		Object o = bridgeUnderTest.get( "date", testDocument );
		assertTrue(
				"The date bridge should return Date instance from a Document not " + o.getClass(), o instanceof Date
		);

		Date actualDate = (Date) o;
		assertEquals( "Added and retrieved dates should match", testDate, actualDate );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1752")
	public void testInvalidDateFieldThrowsException() {
		try {
			bridgeUnderTest.get( "invalidDate", testDocument );
			fail( "The field value is not a valid date and conversion should throw an exception" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000240" ) );
		}
	}
}


