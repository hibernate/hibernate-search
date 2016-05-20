/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.filter;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class ElasticsearchFilterIT extends SearchTestBase {

	@Test
	public void testElasticsearchFilter() {
		Session s = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( ElasticsearchQueries.fromJson( "{ 'query': { 'match_all': {} } }" ), Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );

		assertThat( ftQuery.list() ).onProperty( "name" ).containsOnly( "Liz", "Emmanuel" );

		TermQuery termQuery = new TermQuery( new Term( "name", "liz" ) );
		Filter termFilter = new QueryWrapperFilter( termQuery );
		ftQuery.setFilter( termFilter );

		assertThat( ftQuery.list() ).onProperty( "name" ).containsOnly( "Liz" );

		tx.commit();
		s.close();
	}

	@Test
	public void testElasticsearchFilterWithParameters() {
		Session s = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( ElasticsearchQueries.fromJson( "{ 'query': { 'match_all': {} } }" ), Driver.class );
		ftQuery.enableFullTextFilter( "namedDriver" )
				.setParameter( "name", "liz" );

		assertThat( ftQuery.list() ).onProperty( "name" ).containsOnly( "Liz" );

		tx.commit();
		s.close();
	}

	@Test
	public void testMixedFilters() {
		Session s = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( ElasticsearchQueries.fromJson( "{ 'query': { 'match_all': {} } }" ), Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );

		ftQuery.enableFullTextFilter( "fieldConstraintFilter-1" )
				.setParameter( "field", "teacher" )
				.setParameter( "value", "andre" );

		assertThat( ftQuery.list() ).onProperty( "name" ).containsOnly( "Emmanuel" );

		tx.commit();
		s.close();
	}

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Calendar cal = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT );
		cal.set( 2006, 10, 11 );
		Driver driver = new Driver();
		driver.setDelivery( cal.getTime() );
		driver.setId( 1 );
		driver.setName( "Emmanuel" );
		driver.setScore( 5 );
		driver.setTeacher( "andre" );
		s.persist( driver );

		cal.set( 2007, 10, 11 );
		driver = new Driver();
		driver.setDelivery( cal.getTime() );
		driver.setId( 2 );
		driver.setName( "Gavin" );
		driver.setScore( 3 );
		driver.setTeacher( "aaron" );
		s.persist( driver );

		cal.set( 2004, 10, 11 );
		driver = new Driver();
		driver.setDelivery( cal.getTime() );
		driver.setId( 3 );
		driver.setName( "Liz" );
		driver.setScore( 5 );
		driver.setTeacher( "max" );
		s.persist( driver );

		tx.commit();
		s.close();
	}

	@After
	public void deleteTestData() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		//TODO verify this is no longer needed after we implement the delete operations
		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Driver.class };
	}
}
