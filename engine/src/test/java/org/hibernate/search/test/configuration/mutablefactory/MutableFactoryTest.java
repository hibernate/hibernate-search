/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.mutablefactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.queryparser.classic.ParseException;
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
import org.hibernate.search.test.configuration.mutablefactory.generated.Generated;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner.TaskFactory;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
public class MutableFactoryTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void testCreateEmptyFactory() throws Exception {
		integratorResource.create( new SearchConfigurationForTest() );
	}

	@Test
	public void testAddingClassFullModel() throws Exception {
		SearchIntegrator searchIntegrator = integratorResource.create( new SearchConfigurationForTest() );
		searchIntegrator = integratorResource.create( searchIntegrator, A.class );

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

		searchIntegrator = integratorResource.create( searchIntegrator, B.class );

		tc = new TransactionContextForTest();

		doIndexWork( new B( 1, "Noel" ), 1, searchIntegrator, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		hsQuery = searchIntegrator.createHSQuery( luceneQuery, B.class );
		size = hsQuery.queryResultSize();
		assertEquals( 1, size );
	}

	@Test
	public void testAddingClassSimpleAPI() throws Exception {
		SearchIntegrator sf = integratorResource.create( new SearchConfigurationForTest() );

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
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2421 Support statistics with Elasticsearch
	public void testAddingClassSimpleAPIwithJMX() throws Exception {
		SearchIntegrator sf = integratorResource.create(
						new SearchConfigurationForTest()
							.addClass( A.class )
							.addProperty( "hibernate.search.jmx_enabled" , Boolean.TRUE.toString() )
							.addProperty( "hibernate.search.generate_statistics", Boolean.TRUE.toString() )
							.addProperty( "com.sun.management.jmxremote", Boolean.TRUE.toString() ) );

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
	}

	private static void doIndexWork(Object entity, Integer id, SearchIntegrator sfi, TransactionContextForTest tc) {
		Work work = new Work( entity, id, WorkType.INDEX );
		sfi.getWorker().performWork( work, tc );
	}

	@Test
	public void testMultiThreadedAddClasses() throws Exception {
		QueryParser parser = new QueryParser( "name", TestConstants.standardAnalyzer );
		SearchIntegrator sf = integratorResource.create( new SearchConfigurationForTest() );
		int numberOfClasses = 100;
		int numberOfThreads = 10;
		new ConcurrentRunner( numberOfClasses, numberOfThreads,
				new TaskFactory() {
					@Override
					public Runnable createRunnable(int i) throws Exception {
						return new DoAddClass( sf, i );
					}
				}
			)
			.setTimeout( 1, TimeUnit.MINUTES )
			.execute();

		for ( int i = 0; i < numberOfClasses; i++ ) {
			Query luceneQuery = parser.parse( "Emmanuel" + i );
			final Class<?> classByNumber = getClassByNumber( i, sf.getServiceManager() );
			HSQuery hsQuery = sf.createHSQuery( luceneQuery, classByNumber );
			int size = hsQuery.queryResultSize();
			assertEquals( "Expected 1 document for class " + classByNumber, 1, size );
		}
	}

	private static Class<?> getClassByNumber(int i, ServiceManager serviceManager) throws ClassNotFoundException {
		ClassLoaderService classLoaderService = serviceManager.getClassLoaderService();
		Class<?> clazz = classLoaderService.classForName(
					Generated.A0.class.getName().replace( "A0", "A" + i )
			);
		return clazz;
	}

	private static class DoAddClass implements Runnable {
		private final ExtendedSearchIntegrator extendedIntegrator;
		private final int index;
		private final QueryParser parser;

		public DoAddClass(SearchIntegrator si, int index) {
			this.extendedIntegrator = si.unwrap( ExtendedSearchIntegrator.class );
			this.index = index;
			this.parser = new QueryParser( "name", TestConstants.standardAnalyzer );
		}

		@Override
		public void run() {
			try {
				String name = "Emmanuel" + index;
				final Class<?> aClass = MutableFactoryTest.getClassByNumber(
						index,
						extendedIntegrator.getServiceManager()
				);
				System.err.println( "Creating index #" + index + " for class " + aClass );
				extendedIntegrator.addClasses( aClass );
				Object entity = aClass.getConstructor( Integer.class, String.class )
						.newInstance( index, name );
				TransactionContextForTest context = new TransactionContextForTest();
				MutableFactoryTest.doIndexWork( entity, index, extendedIntegrator, context );
				context.end();

				EntityIndexBinding indexBindingForEntity = extendedIntegrator.getIndexBinding( aClass );
				assertNotNull( indexBindingForEntity );
				IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
				assertEquals( 1, indexManagers.length );

				Query luceneQuery = parser.parse( name );
				HSQuery hsQuery = extendedIntegrator.createHSQuery( luceneQuery, aClass );
				assertEquals( "Should have exactly one result for '" + name + "'", 1, hsQuery.queryResultSize() );
			}
			catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException
					| IllegalAccessException | InstantiationException e) {
				throw new IllegalStateException( "Unexpected exception while manipulating dynamically created classes", e );
			}
			catch (ParseException e) {
				throw new IllegalStateException( "Unexpected exception while parsing query", e );
			}
		}
	}

}
