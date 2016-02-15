/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.test;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchClassBridgeIT extends SearchTestBase {

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Calendar dob = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dob.set( 1958, 3, 7, 0, 0, 0 );
		dob.set( Calendar.MILLISECOND, 0 );

		GolfPlayer hergesheimer = new GolfPlayer.Builder()
			.firstName( "Klaus" )
			.lastName( "Hergesheimer" )
			.active( true )
			.dateOfBirth( dob.getTime() )
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
	public void testQueryOnClassBridgeField() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromQueryString( "fullName:\"Klaus Hergesheimer\"" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class ).list();
		assertThat( result ).onProperty( "id" ).describedAs( "Class-brigde provided string field" ).containsOnly( 1L );

		query = ElasticsearchQueries.fromQueryString( "age:34" );
		result = session.createFullTextQuery( query, GolfPlayer.class ).list();
		assertThat( result ).onProperty( "id" ).describedAs( "Class-brigde provided numeric field" ).containsOnly( 1L );

		tx.commit();
		s.close();
	}

	@Test
	public void testProjectionOfClassBridgeField() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromQueryString( "Hergesheimer" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( "fullName", "age" )
				.list();

		assertThat( result ).hasSize( 1 );

		Object[] projection = (Object[]) result.iterator().next();
		assertThat( projection[0] ).describedAs( "fullName" ).isEqualTo( "Klaus Hergesheimer" );
		assertThat( ( (Number) projection[1] ).intValue() ).describedAs( "age" ).isEqualTo( 34 );

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ GolfPlayer.class };
	}
}
