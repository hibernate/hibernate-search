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
import org.hibernate.search.spatial.impl.SpatialQueryBuilderFromPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hibernate Search spatial : Benchmarks with <a href="http://www.geonames.org">GeoNames</a>
 * as data source (nearly 8M points)
 *
 * Test many queries types :
 *
 * - pure distance post filtering
 * - double numeric range
 * - grid
 * - double range + distance
 * - grid + distance
 *
 * To test you must download <a href="http://download.geonames.org/export/dump/FR.zip">FR GeoBames file</a>
 * and extract it at the root directory of Hibernate Search
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class BenchWithGeonames {

	private static String hibernateConfigurationFile = "/org/hibernate/search/test/spatial/hibernate.cfg.xml";
	private static String geonamesDataFile = "FR.txt";

	public static void main(String args[]) {
		LoadGeonames();
		Bench();
		FacetTest();
	}

	public static void LoadGeonames() {
		Session session = null;
		FullTextSession fullTextSession = null;
		BufferedReader buffRead = null;
		SessionFactory sessionFactory = null;
		try {
			sessionFactory = new Configuration().configure(hibernateConfigurationFile).buildSessionFactory();

			session = sessionFactory.openSession();
			session.createSQLQuery("delete from POI").executeUpdate();
			session.beginTransaction();
			fullTextSession = Search.getFullTextSession( session );

			File geonamesFile = new File( geonamesDataFile );
			buffRead = new BufferedReader( new FileReader( geonamesFile ) );
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
					fullTextSession = Search.getFullTextSession( session );
					session.beginTransaction();
					System.out.println( Integer.toString( line_number ) );
				}
				line_number++;
			}
			session.getTransaction().commit();
			fullTextSession.close();
			sessionFactory.close();
			buffRead.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		finally {
			if ( fullTextSession != null && fullTextSession.isOpen() ) {
				Transaction transaction= fullTextSession.getTransaction();
				if(transaction!=null && transaction.isActive()){
					transaction.rollback();
				}
				fullTextSession.close();
			}
			if ( sessionFactory != null && !sessionFactory.isClosed() ) {
				sessionFactory.close();
			}
			try {
				if( buffRead != null) {
					buffRead.close();
				}
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	public static void Bench() {
		Session session = null;
		FullTextSession fullTextSession = null;
		SessionFactory sessionFactory = null;
		try {

			sessionFactory = new Configuration().configure(hibernateConfigurationFile).buildSessionFactory();

			session = sessionFactory.openSession();
			session.beginTransaction();
			fullTextSession = Search.getFullTextSession( session );

			long gridTotalDuration = 0;
			long spatialTotalDuration = 0;
			long doubleRangeTotalDuration = 0;
			long distanceDoubleRangeTotalDuration = 0;

			long gridDocsFetched = 0;
			long spatialDocsFetched = 0;
			long doubleRangeDocsFetched = 0;
			long distanceDoubleRangeDocsFetched = 0;

			org.apache.lucene.search.Query luceneQuery;
			long startTime, endTime, duration;
			FullTextQuery hibQuery;
			List gridResults, rangeResults;
			final QueryBuilder queryBuilder= fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();
			org.apache.lucene.search.Query query;
			final Integer iterations = 2000;
			final Integer warmUp = 50;
			Random random = new Random( 42 );

			for ( int i = 0; i < iterations; i++ ) {
				Point center = Point.fromDegrees( random.nextDouble() * 2 + 44 , random.nextDouble() * 2 + 3 );
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
					doubleRangeDocsFetched += hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > warmUp ) {
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
					distanceDoubleRangeDocsFetched += hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > warmUp ) {
					distanceDoubleRangeTotalDuration += duration;
				}
				rangeResults= hibQuery.list();
				session.clear();

				luceneQuery = SpatialQueryBuilderFromPoint.buildGridQuery( center, radius, "location" );
				hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
				hibQuery.setProjection( "id", "name" );
				startTime = System.nanoTime();

				try {
					gridDocsFetched += hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > warmUp ) {
					gridTotalDuration += duration;
				}
				session.clear();

				luceneQuery = SpatialQueryBuilderFromPoint.buildSpatialQueryByGrid( center, radius, "location" );
				hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
				hibQuery.setProjection( "id", "name" );
				startTime = System.nanoTime();

				try {
					spatialDocsFetched += hibQuery.getResultSize();
				}
				finally {
					endTime = System.nanoTime();
				}
				duration = endTime - startTime;
				if ( i > warmUp ) {
					spatialTotalDuration += duration;
				}
				gridResults= hibQuery.list();
				session.clear();

				if ( rangeResults.size() != gridResults.size() ) {
					luceneQuery = SpatialQueryBuilderFromPoint.buildDistanceQuery( center, radius, "location" );
					hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
					hibQuery.setProjection( "id", "name" );

					System.out.println(
							">>>>> Different numbers of documents fetched for point (" +
									Double.toString( center.getLatitude()) +
									"," +
									Double.toString( center.getLongitude()) +
									") and radius " +
									Double.toString( radius )
					);
					System.out.println( "Range results : " + rangeResults );
					System.out.println( "Grid results : " + gridResults );
					System.out.println( "Pure distance results : " + hibQuery.getResultSize());

					List<Integer> rangeIds = new ArrayList<Integer>();
					for( int index= 0; index< rangeResults.size(); index++) {
						rangeIds.add((Integer)((Object [])rangeResults.get( index ))[0]);
					}
					List<Integer> gridIds = new ArrayList<Integer>();
					for( int index= 0; index< gridResults.size(); index++) {
						gridIds.add((Integer)((Object [])gridResults.get( index ))[0]);
					}

					rangeIds.removeAll( gridIds );

					System.out.println( "Missing Ids : " + rangeIds);
				}

			}
			session.getTransaction().commit();
			session.close();
			sessionFactory.close();

			System.out
					.println(
							"Mean time with Grid : " + Double.toString(
									( double ) gridTotalDuration * Math.pow( 10, -6 ) / (iterations - warmUp)
							) + " ms. Average number of docs  fetched : " + Double.toString( gridDocsFetched / ((iterations - warmUp) * 1.0d) )
					);
			System.out
					.println(
							"Mean time with Grid + Distance filter : " + Double.toString(
									( double ) spatialTotalDuration * Math.pow( 10, -6 ) / (iterations - warmUp)
							) + " ms. Average number of docs  fetched : " + Double.toString( spatialDocsFetched / ((iterations - warmUp) * 1.0d) )
					);
			System.out
					.println(
							"Mean time with DoubleRange : " + Double.toString(
									( double ) doubleRangeTotalDuration * Math.pow( 10, -6 ) / (iterations - warmUp)
							) + " ms. Average number of docs  fetched : " + Double.toString( doubleRangeDocsFetched / ((iterations - warmUp) * 1.0d) )
					);
			System.out
					.println(
							"Mean time with DoubleRange + Distance filter : " + Double.toString(
									( double ) distanceDoubleRangeTotalDuration * Math.pow( 10, -6 ) / (iterations - warmUp)
							) + " ms. Average number of docs  fetched : " + Double.toString( distanceDoubleRangeDocsFetched / ((iterations - warmUp) * 1.0d) )
					);

		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		finally {
			if ( fullTextSession != null && fullTextSession.isOpen() ) {
				Transaction transaction= fullTextSession.getTransaction();
				if(transaction!=null && transaction.isActive()){
					transaction.rollback();
				}
				fullTextSession.close();
			}
			if ( sessionFactory != null && !sessionFactory.isClosed() ) {
				sessionFactory.close();
			}
		}
	}

	public static void FacetTest() {
		Session session = null;
		FullTextSession fullTextSession = null;

		SessionFactory sessionFactory = null;

		try {
			sessionFactory = new Configuration().configure(hibernateConfigurationFile).buildSessionFactory();

			session = sessionFactory.openSession();
			session.beginTransaction();
			fullTextSession = Search.getFullTextSession( session );

			org.apache.lucene.search.Query luceneQuery;

			FullTextQuery hibQuery;

			Point center = Point.fromDegrees( 46, 4 );
			double radius = 50.0d;

			luceneQuery = SpatialQueryBuilderFromPoint.buildSpatialQueryByGrid( center, radius, "location" );
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

			Integer size = hibQuery.getResultSize();

			List list = hibQuery.list();

			List<Facet> facets = facetManager.getFacets( "typeFacet" );

			System.out.println( facets );

			session.getTransaction().commit();
			session.close();
			sessionFactory.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		finally {
			if ( fullTextSession != null && fullTextSession.isOpen() ) {
				Transaction transaction= fullTextSession.getTransaction();
				if(transaction!=null && transaction.isActive()){
					transaction.rollback();
				}
				fullTextSession.close();
			}
			if ( sessionFactory != null && !sessionFactory.isClosed() ) {
				sessionFactory.close();
			}
		}
	}
}
