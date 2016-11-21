/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.mutablefactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.test.configuration.mutablefactory.generated.Generated;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.hibernate.search.util.impl.Executors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
public class MutableFactoryTest {

	@Test
	public void testCreateEmptyFactory() throws Exception {
		try ( SearchIntegrator si = new SearchIntegratorBuilder().configuration( new SearchConfigurationForTest() ).buildSearchIntegrator() ) {
			// no-op
		}
	}

	@Test
	public void testAddingClassFullModel() throws Exception {
		SearchIntegrator searchIntegrator = new SearchIntegratorBuilder().configuration( new SearchConfigurationForTest() ).buildSearchIntegrator();
		final SearchIntegratorBuilder builder = new SearchIntegratorBuilder();
		searchIntegrator = builder.currentSearchIntegrator( searchIntegrator )
				.addClass( A.class )
				.buildSearchIntegrator();

		TransactionContextForTest tc = new TransactionContextForTest();

		doIndexWork( new A( 1, "Emmanuel" ), 1, searchIntegrator, tc );

		tc.end();

		QueryParser parser = new QueryParser(
				"name",
				TestConstants.standardAnalyzer
		);
		Query luceneQuery = parser.parse( "Emmanuel" );

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, A.class );
		int size = hsQuery.queryResultSize();
		assertEquals( 1, size );

		searchIntegrator = builder.currentSearchIntegrator( searchIntegrator )
				.addClass( B.class )
				.buildSearchIntegrator();

		tc = new TransactionContextForTest();

		doIndexWork( new B( 1, "Noel" ), 1, searchIntegrator, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		hsQuery = searchIntegrator.createHSQuery( luceneQuery, B.class );
		size = hsQuery.queryResultSize();
		assertEquals( 1, size );

		searchIntegrator.close();
	}

	@Test
	public void testAddingClassSimpleAPI() throws Exception {
		SearchIntegrator sf = new SearchIntegratorBuilder().configuration( new SearchConfigurationForTest() ).buildSearchIntegrator();

		sf.addClasses( A.class );

		TransactionContextForTest tc = new TransactionContextForTest();

		doIndexWork( new A( 1, "Emmanuel" ), 1, sf, tc );

		tc.end();

		QueryParser parser = new QueryParser( "name", TestConstants.standardAnalyzer );
		Query luceneQuery = parser.parse( "Emmanuel" );

		HSQuery hsQuery = sf.createHSQuery( luceneQuery, A.class );
		int size = hsQuery.queryResultSize();
		assertEquals( 1, size );

		sf.addClasses( B.class, C.class );

		tc = new TransactionContextForTest();

		doIndexWork( new B( 1, "Noel" ), 1, sf, tc );
		doIndexWork( new C( 1, "Vincent" ), 1, sf, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		hsQuery = sf.createHSQuery( luceneQuery, B.class );
		size = hsQuery.queryResultSize();
		assertEquals( 1, size );

		luceneQuery = parser.parse( "Vincent" );

		hsQuery = sf.createHSQuery( luceneQuery, C.class );
		size = hsQuery.queryResultSize();
		assertEquals( 1, size );

		sf.close();
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2421 Support statistics with Elasticsearch
	public void testAddingClassSimpleAPIwithJMX() throws Exception {
		SearchIntegrator sf = new SearchIntegratorBuilder()
				.configuration(
						new SearchConfigurationForTest()
							.addClass( A.class )
							.addProperty( "hibernate.search.jmx_enabled" , Boolean.TRUE.toString() )
							.addProperty( "hibernate.search.generate_statistics", Boolean.TRUE.toString() )
							.addProperty( "com.sun.management.jmxremote", Boolean.TRUE.toString() ) )
				.buildSearchIntegrator();

		TransactionContextForTest tc = new TransactionContextForTest();

		doIndexWork( new A( 1, "Emmanuel" ), 1, sf, tc );

		tc.end();

		QueryParser parser = new QueryParser( "name", TestConstants.standardAnalyzer );
		Query luceneQuery = parser.parse( "Emmanuel" );

		HSQuery hsQuery = sf.createHSQuery( luceneQuery, A.class );

		hsQuery.getTimeoutManager().start();

		List<EntityInfo> entityInfos = hsQuery.queryEntityInfos();
		assertEquals( 1 , entityInfos.size() );
		assertEquals( 1 , sf.getStatistics().getSearchQueryExecutionCount() );
		assertEquals( 1 , sf.getStatistics().getIndexedClassNames().size() );

		hsQuery.getTimeoutManager().stop();

		sf.addClasses( B.class, C.class );

		tc = new TransactionContextForTest();

		doIndexWork( new B( 1, "Noel" ), 1, sf, tc );
		doIndexWork( new C( 1, "Vincent" ), 1, sf, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		hsQuery = sf.createHSQuery( luceneQuery, A.class, B.class, C.class );

		hsQuery.getTimeoutManager().start();

		entityInfos = hsQuery.queryEntityInfos();
		assertEquals( 1 , entityInfos.size() );
		assertEquals( 2 , sf.getStatistics().getSearchQueryExecutionCount() );
		assertEquals( 3 , sf.getStatistics().getIndexedClassNames().size() );

		hsQuery.getTimeoutManager().stop();

		luceneQuery = parser.parse( "Vincent" );

		hsQuery = sf.createHSQuery( luceneQuery, A.class, B.class, C.class );

		hsQuery.getTimeoutManager().start();

		entityInfos = hsQuery.queryEntityInfos();
		assertEquals( 1 , entityInfos.size() );
		assertEquals( 3 , sf.getStatistics().getSearchQueryExecutionCount() );
		assertEquals( 3 , sf.getStatistics().getIndexedClassNames().size() );

		hsQuery.getTimeoutManager().stop();

		sf.close();
	}

	private static void doIndexWork(Object entity, Integer id, SearchIntegrator sfi, TransactionContextForTest tc) {
		Work work = new Work( entity, id, WorkType.INDEX );
		sfi.getWorker().performWork( work, tc );
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2480 Adding classes to the SearchIntegrator concurrently fails with Elasticsearch
	public void testMultiThreadedAddClasses() throws Exception {
		QueryParser parser = new QueryParser( "name", TestConstants.standardAnalyzer );
		try ( SearchIntegrator sf = new SearchIntegratorBuilder().configuration( new SearchConfigurationForTest() ).buildSearchIntegrator() ) {
			List<DoAddClasses> runnables = new ArrayList<DoAddClasses>( 10 );
			final int nbrOfThread = 10;
			final int nbrOfClassesPerThread = 10;
			for ( int i = 0; i < nbrOfThread; i++ ) {
				runnables.add( new DoAddClasses( sf, i, nbrOfClassesPerThread ) );
			}
			final ThreadPoolExecutor poolExecutor = Executors.newFixedThreadPool( nbrOfThread, "SFI classes addition" );
			poolExecutor.prestartAllCoreThreads();
			for ( Runnable runnable : runnables ) {
				poolExecutor.execute( runnable );
			}
			poolExecutor.shutdown();

			if ( !poolExecutor.awaitTermination( 1, TimeUnit.MINUTES ) ) {
				poolExecutor.shutdownNow();
				fail( "The thread pool didn't finish executing after 1 minute" );
			}

			AssertionError reportedError = null;
			for ( DoAddClasses runnable : runnables ) {
				Throwable throwableFromCurrentRunnable = runnable.getThrowable();
				if ( throwableFromCurrentRunnable != null ) {
					if ( reportedError == null ) {
						reportedError = new AssertionError( "Unexpected failure on thread #" + runnable.getWorkNumber(), throwableFromCurrentRunnable );
					}
					else {
						reportedError.addSuppressed( throwableFromCurrentRunnable );
					}
				}
			}

			if ( reportedError != null ) {
				throw reportedError;
			}

			for ( int i = 0; i < nbrOfThread * nbrOfClassesPerThread; i++ ) {
				Query luceneQuery = parser.parse( "Emmanuel" + i );
				final Class<?> classByNumber = getClassByNumber( i, sf.getServiceManager() );
				HSQuery hsQuery = sf.createHSQuery( luceneQuery, classByNumber );
				int size = hsQuery.queryResultSize();
				assertEquals( "Expected 1 document for class " + classByNumber, 1, size );
			}
		}
	}

	private static Class<?> getClassByNumber(int i, ServiceManager serviceManager) throws ClassNotFoundException {
		ClassLoaderService classLoaderService = serviceManager.getClassLoaderService();
		Class<?> clazz = classLoaderService.classForName(
					Generated.A0.class.getName().replace( "A0", "A" + i )
			);
		return clazz;
	}

	private static class DoAddClasses implements Runnable {
		private final ExtendedSearchIntegrator extendedIntegrator;
		private final int factorOfClassesPerThread;
		private final QueryParser parser;
		private final int nbrOfClassesPerThread;
		private volatile Throwable throwable;

		public Throwable getThrowable() {
			return throwable;
		}

		public int getWorkNumber() {
			return factorOfClassesPerThread;
		}

		public DoAddClasses(SearchIntegrator si, int factorOfClassesPerThread, int nbrOfClassesPerThread) {
			this.extendedIntegrator = si.unwrap( ExtendedSearchIntegrator.class );
			this.factorOfClassesPerThread = factorOfClassesPerThread;
			this.parser = new QueryParser( "name", TestConstants.standardAnalyzer );
			this.nbrOfClassesPerThread = nbrOfClassesPerThread;
		}

		@Override
		public void run() {
			try {
				for ( int index = 0; index < 10; index++ ) {
					final int i = factorOfClassesPerThread * nbrOfClassesPerThread + index;
					String name = "Emmanuel" + i;
					final Class<?> aClass = MutableFactoryTest.getClassByNumber(
							i,
							extendedIntegrator.getServiceManager()
					);
					System.err.println( "Creating index #" + i + " for class " + aClass );
					extendedIntegrator.addClasses( aClass );
					Object entity = aClass.getConstructor( Integer.class, String.class )
							.newInstance( i, name );
					TransactionContextForTest context = new TransactionContextForTest();
					MutableFactoryTest.doIndexWork( entity, i, extendedIntegrator, context );
					context.end();

					EntityIndexBinding indexBindingForEntity = extendedIntegrator.getIndexBinding( aClass );
					assertNotNull( indexBindingForEntity );
					IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
					assertEquals( 1, indexManagers.length );

					Query luceneQuery = parser.parse( name );
					HSQuery hsQuery = extendedIntegrator.createHSQuery( luceneQuery, aClass );
					assertEquals( "Should have exactly one result for '" + name + "'", 1, hsQuery.queryResultSize() );
				}
			}
			catch (Exception | AssertionError t) {
				this.throwable = t;
			}
		}
	}

}
