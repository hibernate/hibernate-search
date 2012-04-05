package org.hibernate.search.test.spatial;

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.QueryWrapperFilter;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spatial.impl.Rectangle;
import org.hibernate.search.spatial.SpatialQueryBuilder;
import org.hibernate.search.spatial.impl.SpatialQueryBuilderFromPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Hibernate Search spatial : Benchmarks with {@link http://www.geonames.org} as data source (nearly 8M points)
 *
 * Test many queries types :
 *
 * - pure distance post filtering
 * - double numeric range
 * - grid
 * - double range + distance
 * - grid + distance
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class BenchWithGeonames {
	public static void main(String args[]) {
		Session session = null;
		FullTextSession fullTextSession = null;
		try {

			SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

			session = sessionFactory.openSession();
			session.beginTransaction();
			fullTextSession = Search.getFullTextSession( session );

			long gridTotalDuration = 0;
			long spatialTotalDuration = 0;
			long doubleRangeTotalDuration = 0;
			long distanceDoubleRangeTotalDuration = 0;

			org.apache.lucene.search.Query luceneQuery;
			long startTime, endTime, duration;
			FullTextQuery hibQuery;
			List results;
			final QueryBuilder queryBuilder= fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();
			org.apache.lucene.search.Query query;
			final Integer iterations = 200;
			final Integer warmUp = 25;

			for ( int i = 0; i < iterations; i++ ) {
				Point center = Point.fromDegrees( Math.random() * 2 + 44 , Math.random() * 2 + 3 );
				double radius = 25.0d;
				Rectangle boundingBox = Rectangle.fromBoundingCircle( center, radius );

				query = queryBuilder.bool()
						.must(
								queryBuilder.range()
										.onField( "latitude" )
										.from( boundingBox.getLowerLeft().getLatitude() )
										.to( boundingBox.getUpperRight().getLatitude() )
										.createQuery()
						)
						.must(
								queryBuilder.range()
										.onField( "longitude" )
										.from( boundingBox.getLowerLeft().getLongitude() )
										.to( boundingBox.getUpperRight().getLongitude() )
										.createQuery()
						)
						.createQuery();
				hibQuery = fullTextSession.createFullTextQuery( query, POI.class );
				hibQuery.setProjection( "id", "name" );
				startTime = System.nanoTime();
				try {
					hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > ( iterations - warmUp ) ) {
					doubleRangeTotalDuration += duration;
				}
				session.clear();

				query = queryBuilder.bool()
						.must(
								queryBuilder.range()
										.onField( "latitude" )
										.from( boundingBox.getLowerLeft().getLatitude() )
										.to( boundingBox.getUpperRight().getLatitude() )
										.createQuery()
						)
						.must(
								queryBuilder.range()
										.onField( "longitude" )
										.from( boundingBox.getLowerLeft().getLongitude() )
										.to( boundingBox.getUpperRight().getLongitude() )
										.createQuery()
						)
						.createQuery();
				org.apache.lucene.search.Query filteredQuery = new ConstantScoreQuery(
						SpatialQueryBuilderFromPoint.buildDistanceFilter(
								new QueryWrapperFilter( query ),
								center,
								radius,
								"location"
						)
				);
				hibQuery = fullTextSession.createFullTextQuery( filteredQuery, POI.class );
				hibQuery.setProjection( "id", "name" );
				startTime = System.nanoTime();
				try {
					hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > ( iterations - warmUp ) ) {
					distanceDoubleRangeTotalDuration += duration;
				}
				session.clear();

				luceneQuery = SpatialQueryBuilderFromPoint.buildGridQuery( center, radius, "location" );
				hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
				hibQuery.setProjection( "id", "name" );
				startTime = System.nanoTime();

				try {
					hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > ( iterations - warmUp ) ) {
					gridTotalDuration += duration;
				}
				session.clear();

				luceneQuery = SpatialQueryBuilderFromPoint.buildSpatialQuery( center, radius, "location" );
				hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
				hibQuery.setProjection( "id", "name" );
				startTime = System.nanoTime();

				try {
					hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > ( iterations - warmUp ) ) {
					spatialTotalDuration += duration;
				}
				session.clear();
			}
			session.getTransaction().commit();
			session.close();

			System.out
					.println(
							" Mean time with Grid : " + Double.toString(
									( double ) gridTotalDuration / 100.0d * Math.pow(
											10,
											-9
									)
							)
					);
			System.out
					.println(
							" Mean time with Grid + Distance filter : " + Double.toString(
									( double ) spatialTotalDuration / 100.0d * Math.pow( 10, -9 )
							)
					);
			System.out
					.println(
							" Mean time with DoubleRange : " + Double.toString(
									( double ) doubleRangeTotalDuration / 100.0d * Math.pow( 10, -9 )
							)
					);
			System.out
					.println(
							" Mean time with DoubleRange + Distance filter : " + Double.toString(
									( double ) distanceDoubleRangeTotalDuration / 100.0d * Math.pow( 10, -9 )
							)
					);

		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		finally {
			if ( session != null && session.isOpen() ) {
				Transaction transaction= session.getTransaction();
				if(transaction!=null && transaction.isActive()){
					transaction.rollback();
				}
				session.close();
			}
		}
	}

	//@Test
	public void LoadGeonames() {
		Session session = null;
		FullTextSession fullTextSession = null;
		try {
			SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

			session = sessionFactory.openSession();
			session.beginTransaction();
			fullTextSession = Search.getFullTextSession( session );

			String geonamesFileName = "geonames\\FR.txt";
			File geonamesFiles = new File( geonamesFileName );
			BufferedReader buffRead = new BufferedReader( new FileReader( geonamesFiles ) );
			String line = null;

			int line_number = 0;
			while ( ( line = buffRead.readLine() ) != null ) {
				String[] data = line.split( "\t" );
				POI current = new POI(
						Integer.parseInt( data[0] ),
						data[1],
						Double.parseDouble( data[4] ),
						Double.parseDouble( data[5] ),
						data[7]
				);
				session.save( current );
				if ( ( line_number % 10000 ) == 0 ) {
					fullTextSession.flushToIndexes();
					session.getTransaction().commit();
					session.close();
					session = sessionFactory.openSession();
					session.beginTransaction();
					fullTextSession = Search.getFullTextSession( session );
					session.beginTransaction();
					System.out.println( Integer.toString( line_number ) );
				}
				line_number++;
			}
			session.getTransaction().commit();
			session.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		finally {
			if ( session != null && session.isOpen() ) {
				session.close();
			}
		}
	}

	//@Test
	public void FacetTest() {
		Session session = null;
		FullTextSession fullTextSession = null;

		SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

		session = sessionFactory.openSession();
		session.beginTransaction();
		fullTextSession = Search.getFullTextSession( session );

		org.apache.lucene.search.Query luceneQuery;

		FullTextQuery hibQuery;

		Point center = Point.fromDegrees( 46, 4 );
		double radius = 50.0d;

		luceneQuery = SpatialQueryBuilderFromPoint.buildSpatialQuery( center, radius, "location" );
		hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		hibQuery.setProjection( "id", "name", "type" );

		FacetManager facetManager = hibQuery.getFacetManager();

		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

		FacetingRequest facetingRequest = queryBuilder.facet()
				.name( "typeFacet" )
				.onField( "type" )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.createFacetingRequest();

		facetManager.enableFaceting( facetingRequest );


		try {

			Integer size = hibQuery.getResultSize();

			List list = hibQuery.list();

			List<Facet> facets = facetManager.getFacets( "typeFacet" );

			System.out.println( facets );

		}
		catch ( Exception e ) {

		}
	}
}
