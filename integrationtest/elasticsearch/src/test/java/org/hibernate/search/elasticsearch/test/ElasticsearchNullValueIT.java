/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchNullValueIT extends SearchTestBase {

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
	public void testQueryOnNullToken() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromQueryString( "firstName:&lt;NULL&gt;" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class ).list();
		assertThat( result ).onProperty( "id" ).describedAs( "Querying null-encoded String" ).containsOnly( 2L );

		query = ElasticsearchQueries.fromQueryString( "dateOfBirth:1970-01-01" );
		result = session.createFullTextQuery( query, GolfPlayer.class ).list();
		assertThat( result ).onProperty( "id" ).describedAs( "Querying null-encoded Date" ).containsOnly( 2L );

		query = ElasticsearchQueries.fromQueryString( "active:false" );
		result = session.createFullTextQuery( query, GolfPlayer.class ).list();
		assertThat( result ).onProperty( "id" ).describedAs( "Querying null-encoded Boolean" ).containsOnly( 2L );

		query = ElasticsearchQueries.fromQueryString( "driveWidth:\\-1" );
		result = session.createFullTextQuery( query, GolfPlayer.class ).list();
		assertThat( result ).onProperty( "id" ).describedAs( "Querying null-encoded Integer" ).containsOnly( 2L );

		tx.commit();
		s.close();
	}

	@Test
	public void testProjectionOfNullValues() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromQueryString( "lastName:Kidd" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection(
						"firstName",
						"lastName",
						"active",
						"dateOfBirth",
						"handicap",
						"driveWidth",
						"ranking.value"
				)
				.list();

		assertThat( result ).hasSize( 1 );

		Object[] projection = (Object[]) result.iterator().next();
		assertThat( projection[0] ).describedAs( "firstName" ).isNull();
		assertThat( projection[1] ).describedAs( "lastName" ).isEqualTo( "Kidd" );
		assertThat( projection[2] ).describedAs( "active" ).isNull();
		assertThat( projection[3] ).describedAs( "dateOfBirth" ).isNull();
		assertThat( projection[4] ).describedAs( "handicap" ).isEqualTo( 0.0D );
		assertThat( projection[5] ).describedAs( "driveWidth" ).isNull();
		assertThat( projection[6] ).describedAs( "ranking value" ).isNull();

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ GolfPlayer.class, GolfCourse.class, Hole.class };
	}
}
