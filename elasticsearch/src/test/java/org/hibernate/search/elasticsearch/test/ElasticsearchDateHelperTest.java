/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.elasticsearch.util.impl.ElasticsearchDateHelper;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

import org.apache.lucene.document.DateTools;

public class ElasticsearchDateHelperTest {

	@Test
	public void formatAndParse() {
		Calendar dateOfBirth = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dateOfBirth.set( 1958, Calendar.APRIL, 7, 0, 0, 0 );
		dateOfBirth.set( Calendar.MILLISECOND, 0 );
		assertSameTimeAfterFormatAndParse( dateOfBirth );

		Calendar subscriptionEndDate = GregorianCalendar.getInstance( TimeZone.getTimeZone( "Europe/Paris" ), Locale.FRENCH );
		subscriptionEndDate.set( 2016, Calendar.JUNE, 7, 4, 4, 4 );
		assertSameTimeAfterFormatAndParse( subscriptionEndDate );
	}

	/**
	 * Check that dates and calendars formatted in Hibernate Search 5.10 and below,
	 * with a format that does not include the ".0" suffix when milliseconds are 0,
	 * still works with the new parser/formatter.
	 *
	 * Data sets taken from Elasticsearch5IndexMappingIT.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3237")
	public void parseHibernateSearch510AndBelowFormat() {
		Calendar dateOfBirth = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dateOfBirth.set( 1958, Calendar.APRIL, 7, 0, 0, 0 );
		dateOfBirth.set( Calendar.MILLISECOND, 0 );
		dateOfBirth = ElasticsearchDateHelper.round( dateOfBirth, DateTools.Resolution.DAY );
		assertEquals(
				dateOfBirth.getTime(),
				ElasticsearchDateHelper.stringToCalendar( "1958-04-07T00:00:00Z" ).getTime()
		);
		assertEquals(
				dateOfBirth.getTime(),
				ElasticsearchDateHelper.stringToDate( "1958-04-07T00:00:00Z" )
		);

		Calendar subscriptionEndDate = GregorianCalendar.getInstance( TimeZone.getTimeZone( "Europe/Paris" ), Locale.FRENCH );
		subscriptionEndDate.set( 2016, Calendar.JUNE, 7, 4, 4, 4 );
		subscriptionEndDate = ElasticsearchDateHelper.round( subscriptionEndDate, DateTools.Resolution.DAY );
		assertEquals(
				subscriptionEndDate.getTime(),
				ElasticsearchDateHelper.stringToCalendar( "2016-06-07T02:00:00+02:00" ).getTime()
		);
		assertEquals(
				subscriptionEndDate.getTime(),
				ElasticsearchDateHelper.stringToDate( "2016-06-07T02:00:00+02:00" )
		);
	}

	private void assertSameTimeAfterFormatAndParse(Calendar calendar) {
		assertEquals(
				calendar.getTime(),
				ElasticsearchDateHelper.stringToCalendar( ElasticsearchDateHelper.calendarToString( calendar ) ).getTime()
		);

		Date date = calendar.getTime();
		assertEquals(
				date,
				ElasticsearchDateHelper.stringToDate( ElasticsearchDateHelper.dateToString( date ) )
		);
	}

}
