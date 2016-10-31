/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class ElasticsearchDateCalendarBridgeIT extends SearchTestBase {

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Calendar dob = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dob.set( 1958, 3, 7, 5, 5, 5 );
		dob.set( Calendar.MILLISECOND, 0 );

		Calendar subscriptionEndDate = GregorianCalendar.getInstance( TimeZone.getTimeZone( "Europe/Paris" ), Locale.FRENCH );
		subscriptionEndDate.set( 2016, 5, 7, 4, 4, 4 );

		GolfPlayer hergesheimer = new GolfPlayer.Builder()
				.firstName( "Klaus" )
				.lastName( "Hergesheimer" )
				.active( true )
				.dateOfBirth( dob.getTime() )
				.subscriptionEndDate( subscriptionEndDate )
				.handicap( 3.4 )
				.driveWidth( 285 )
				.ranking( 311 )
				.build();
		s.persist( hergesheimer );

		GolfPlayer kidd = new GolfPlayer.Builder()
				.lastName( "Kidd" )
				.build();
		s.persist( kidd );

		tx.commit();
		s.close();
	}

	@After
	public void deleteTestData() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}

		tx.commit();
		s.close();
	}

	@Test
	public void testDateResolution() {
		Session s = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		Calendar dob = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dob.set( 1958, 3, 7, 7, 7, 7 );

		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( GolfPlayer.class ).get();

		Query query = monthQb.keyword().onField( "dateOfBirth" ).matching( dob.getTime() ).createQuery();
		assertEquals( 1, fullTextSession.createFullTextQuery( query, GolfPlayer.class ).getResultSize() );

		tx.commit();
		s.close();
	}

	@Test
	public void testCalendarResolution() {
		Session s = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		Calendar subscriptionEndDate = GregorianCalendar.getInstance( TimeZone.getTimeZone( "Europe/Paris" ), Locale.FRENCH );
		// Expecting to match as resolution is DAY
		subscriptionEndDate.set( 2016, 5, 7, 7, 7, 7 );

		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( GolfPlayer.class ).get();

		Query query = monthQb.keyword().onField( "subscriptionEndDate" ).matching( subscriptionEndDate ).createQuery();
		assertEquals( 1, fullTextSession.createFullTextQuery( query, GolfPlayer.class ).getResultSize() );

		tx.commit();
		s.close();
	}

	@Test
	public void testProjectionOfCalendarValueRetrievesCorrectTimeZoneOffset() {
		Session s = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		Calendar subscriptionEndDate = GregorianCalendar.getInstance( TimeZone.getTimeZone( "Europe/Paris" ), Locale.FRENCH );
		// Expecting to match as resolution is DAY
		subscriptionEndDate.set( 2016, 5, 7, 7, 7, 7 );
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( GolfPlayer.class ).get();

		Query query = monthQb.keyword().onField( "subscriptionEndDate" ).matching( subscriptionEndDate ).createQuery();

		@SuppressWarnings("unchecked")
		List<Object[]> results = fullTextSession.createFullTextQuery( query, GolfPlayer.class ).setProjection( "subscriptionEndDate" ).list();
		assertEquals( 1, results.size() );

		long subscriptionEndDateTime = subscriptionEndDate.getTime().getTime();
		assertEquals(
				TimeZone.getTimeZone( "Europe/Paris" ).getOffset( subscriptionEndDateTime ),
				( (Calendar) results.iterator().next()[0] ).getTimeZone().getOffset( subscriptionEndDateTime )
		);

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ GolfPlayer.class, GolfCourse.class, Hole.class };
	}
}
