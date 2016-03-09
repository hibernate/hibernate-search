/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic smoke test to make sure the different configuration settings can be overridden.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchConfigurationValuesSpecifiedIT extends SearchTestBase {

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		GolfPlayer hergesheimer = new GolfPlayer.Builder()
			.firstName( "Klaus" )
			.lastName( "Hergesheimer" )
			.build();

		s.persist( hergesheimer );

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
	public void canQueryEntityWithConfigurationValuesGiven() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class ).list();

		assertThat( result ).onProperty( "firstName" ).containsOnly( "Klaus" );

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { GolfPlayer.class, GolfCourse.class, Hole.class };
	}

	@Override
	public void configure(Map<String, Object> settings) {
		settings.put( ElasticsearchEnvironment.SERVER_URI, "http://127.0.0.1:9200" );
		settings.put( ElasticsearchEnvironment.INDEX_MANAGEMENT_WAIT_TIMEOUT, 5_000 );
		settings.put( ElasticsearchEnvironment.INDEX_MANAGEMENT_STRATEGY, "CREATE" );
	}
}
