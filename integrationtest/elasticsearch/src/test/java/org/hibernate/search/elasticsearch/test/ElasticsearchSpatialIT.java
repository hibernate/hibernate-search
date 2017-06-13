/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.apache.lucene.search.Sort;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.spatial.POI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class ElasticsearchSpatialIT extends SearchTestBase {

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		s.persist( poi );
		s.persist( poi2 );
		s.persist( poi3 );
		s.persist( poi4 );
		s.persist( poi5 );
		s.persist( poi6 );

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
	public void testGeoDistanceQuery() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		String geoDistanceQuery = "{\n" +
			"'query' : {\n" +
				"'bool' : {\n" +
					"'must' : {\n" +
						"'match_all' : {}\n" +
					"},\n" +
					"'filter' : {\n" +
						"'geo_distance' : {\n" +
							"'distance' : '12km',\n" +
							"'location' : {\n" +
								"'lat' : 24,\n" +
								"'lon' : 32\n" +
							"}\n" +
						"}\n" +
					"}\n" +
				"}\n" +
			"}\n" +
		"}";

		QueryDescriptor query = ElasticsearchQueries.fromJson( geoDistanceQuery );
		List<?> result = session.createFullTextQuery( query, POI.class )
				.setSort( new Sort( new DistanceSortField( 24, 32, "location" ) ) )
				.list();
		assertThat( result ).onProperty( "id" ).describedAs( "Geo distance query" ).containsOnly( 1, 2, 3 );

		tx.commit();
		s.close();
	}

	@Test
	public void testBoundingBoxQuery() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();
		String boundingBoxQuery = "{\n" +
			"'query' : {\n" +
				"'bool' : {\n" +
					"'must' : {\n" +
						"'match_all' : {}\n" +
					"},\n" +
					"'filter' : {\n" +
						"'geo_bounding_box' : {\n" +
							"'location' : {\n" +
								"'top_left' : {\n" +
									"'lat' : 24,\n" +
									"'lon' : 31.8\n" +
								"},\n" +
								"'bottom_right' : {\n" +
									"'lat' : 23.8,\n" +
									"'lon' : 32.1\n" +
								"}\n" +
							"}\n" +
						"}\n" +
					"}\n" +
				"}\n" +
			"}\n" +
		"}";

		QueryDescriptor query = ElasticsearchQueries.fromJson( boundingBoxQuery );
		List<?> result = session.createFullTextQuery( query, POI.class ).list();
		assertThat( result ).onProperty( "id" ).describedAs( "Geo distance query" ).containsOnly( 1, 2, 3, 4 );

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ POI.class };
	}
}
