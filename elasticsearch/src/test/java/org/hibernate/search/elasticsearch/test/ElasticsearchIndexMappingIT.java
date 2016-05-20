/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.elasticsearch.testutil.JsonHelper;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchIndexMappingIT extends SearchTestBase {

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Calendar dob = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dob.set( 1958, 3, 7, 0, 0, 0 );
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
			.puttingStrength( 2.5 )
			.driveWidth( 285 )
			.strength( "precision" )
			.strength( "willingness" )
			.strength( "stamina" )
			.build();
		s.persist( hergesheimer );

		GolfPlayer galore = new GolfPlayer.Builder()
			.lastName( "Galore" )
			.ranking( 311 )
			.build();
		s.persist( galore );

		GolfPlayer kidd = new GolfPlayer.Builder()
			.lastName( "Kidd" )
			.build();
		s.persist( kidd );

		GolfCourse purbeck = new GolfCourse(
				"Purbeck",
				127.3,
				new Hole( 433, (byte) 4 ), new Hole( 163, (byte) 3 )
		);
		s.persist( purbeck );

		GolfCourse mountMaja = new GolfCourse(
				"Mount Maja",
				111.9,
				new Hole( 512, (byte) 5 ), new Hole( 113, (byte) 3 )
		);
		s.persist( mountMaja );

		GolfPlayer brand = new GolfPlayer.Builder()
				.lastName( "Brand" )
				.playedCourses( purbeck, mountMaja )
				.build();
		s.persist( brand );

		purbeck.getPlayedBy().add( brand );
		mountMaja.getPlayedBy().add( brand );

		s.persist( brand );

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
	public void testMapping() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'lastName' : 'Hergesheimer' } } }" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( ElasticsearchProjectionConstants.SOURCE )
				.list();

		String source = (String) ( (Object[]) result.iterator().next() )[0];

		JsonHelper.assertJsonEqualsIgnoringUnknownFields(
				"{" +
					"\"active\": true," +
					"\"dateOfBirth\": \"1958-04-07T00:00:00Z\"," +
					"\"subscriptionEndDate\": \"2016-06-07T00:00:00+02:00\"," +
					"\"driveWidth\": 285," +
					"\"firstName\": \"Klaus\"," +
					"\"handicap\": 3.4," +
					"\"lastName\": \"Hergesheimer\"," +
					"\"fullName\": \"Klaus Hergesheimer\"," +
					"\"age\": 34," +
					"\"puttingStrength\": \"2.5\"" +
				"}",
				source
		);

		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddedMapping() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'lastName' : 'Galore' } } }" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( ElasticsearchProjectionConstants.SOURCE )
				.list();

		String source = (String) ( (Object[]) result.iterator().next() )[0];

		JsonHelper.assertJsonEqualsIgnoringUnknownFields(
				"{" +
					"\"lastName\": \"Galore\"," +
					"\"ranking\": {" +
						"\"value\": \"311\"" +
					"}" +
				"}",
				source
		);

		tx.commit();
		s.close();
	}

	@Test
	public void testElementCollectionOfBasicTypeMapping() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'lastName' : 'Hergesheimer' } } }" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( ElasticsearchProjectionConstants.SOURCE )
				.list();

		String source = (String) ( (Object[]) result.iterator().next() )[0];

		JsonHelper.assertJsonEqualsIgnoringUnknownFields(
				"{" +
					"\"lastName\": \"Hergesheimer\"," +
					"\"strengths\": [" +
						"\"willingness\", \"precision\", \"stamina\"" +
					"]" +
				"}",
				source
		);

		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddedListOfEntityMapping() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'lastName' : 'Brand' } } }" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
						.setProjection( ElasticsearchProjectionConstants.SOURCE )
						.list();

		String source = (String) ( (Object[]) result.iterator().next() )[0];

		JsonHelper.assertJsonEqualsIgnoringUnknownFields(
			"{" +
				"'lastName' : 'Brand'," +
				"'playedCourses': [" +
					"{" +
						"'name' : 'Purbeck'," +
						"'rating' : 127.3, " +
						"'holes': [" +
							"{ 'par' : 4, 'length' : 433 }," +
							"{ 'par' : 3, 'length' : 163 }" +
						"]" +
					"}," +
					"{" +
						"'name' : 'Mount Maja'," +
						"'rating' : 111.9, " +
						"'holes': [" +
							"{ 'par' : 5, 'length' : 512 }," +
							"{ 'par' : 3, 'length' : 113 }" +
						"]" +
					"}" +
				"]" +
			"}",
			source
		);

		tx.commit();
		s.close();
	}

	@Test
	public void testNullTokenMapping() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromQueryString( "lastName:Kidd" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( ElasticsearchProjectionConstants.SOURCE )
				.list();

		String source = (String) ( (Object[]) result.iterator().next() )[0];

		JsonHelper.assertJsonEquals(
				"{" +
					"\"active\": null," +
					"\"dateOfBirth\": null," +
					"\"subscriptionEndDate\":null," +
					"\"driveWidth\": null," +
					"\"firstName\": null," +
					"\"handicap\": 0.0," + // not nullable
					"\"puttingStrength\": \"0.0\"," + // not nullable
					"\"lastName\": \"Kidd\"," +
					"\"fullName\": \"Kidd\"" +
					// ranking.value is null but indexNullAs() has not been given, so it's
					// not present in the index at all
					// "\"ranking\": {" +
					//     "\"value\": ..." +
					// "}" +
				"}",
				source
		);

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { GolfPlayer.class, GolfCourse.class, Hole.class };
	}
}
