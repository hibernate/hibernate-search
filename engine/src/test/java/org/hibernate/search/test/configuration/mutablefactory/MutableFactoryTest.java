/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.mutablefactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.test.configuration.mutablefactory.generated.Generated;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
				TestConstants.getTargetLuceneVersion(),
				"name",
				TestConstants.standardAnalyzer
		);
		Query luceneQuery = parser.parse( "Emmanuel" );

		IndexReader indexReader = searchIntegrator.getIndexReaderAccessor().open( A.class );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searchIntegrator.getIndexReaderAccessor().close( indexReader );

		searchIntegrator = builder.currentSearchIntegrator( searchIntegrator )
				.addClass( B.class )
				.buildSearchIntegrator();

		tc = new TransactionContextForTest();

		doIndexWork( new B( 1, "Noel" ), 1, searchIntegrator, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		indexReader = searchIntegrator.getIndexReaderAccessor().open( B.class );
		searcher = new IndexSearcher( indexReader );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searchIntegrator.getIndexReaderAccessor().close( indexReader );

		searchIntegrator.close();
	}

	@Test
	public void testAddingClassSimpleAPI() throws Exception {
		SearchIntegrator sf = new SearchIntegratorBuilder().configuration( new SearchConfigurationForTest() ).buildSearchIntegrator();

		sf.addClasses( A.class );

		TransactionContextForTest tc = new TransactionContextForTest();

		doIndexWork( new A( 1, "Emmanuel" ), 1, sf, tc );

		tc.end();

		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"name",
				TestConstants.standardAnalyzer
		);
		Query luceneQuery = parser.parse( "Emmanuel" );

		IndexReader indexReader = sf.getIndexReaderAccessor().open( A.class );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		sf.getIndexReaderAccessor().close( indexReader );

		sf.addClasses( B.class, C.class );

		tc = new TransactionContextForTest();

		doIndexWork( new B( 1, "Noel" ), 1, sf, tc );
		doIndexWork( new C( 1, "Vincent" ), 1, sf, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		indexReader = sf.getIndexReaderAccessor().open( B.class );
		searcher = new IndexSearcher( indexReader );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );
		sf.getIndexReaderAccessor().close( indexReader );

		luceneQuery = parser.parse( "Vincent" );

		indexReader = sf.getIndexReaderAccessor().open( C.class );
		searcher = new IndexSearcher( indexReader );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		sf.getIndexReaderAccessor().close( indexReader );

		sf.close();
	}

	private static void doIndexWork(Object entity, Integer id, SearchIntegrator sfi, TransactionContextForTest tc) {
		Work work = new Work( entity, id, WorkType.INDEX );
		sfi.getWorker().performWork( work, tc );
	}

	@Test
	public void testMultiThreadedAddClasses() throws Exception {
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"name",
				TestConstants.standardAnalyzer
		);
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

			boolean inProgress;
			do {
				Thread.sleep( 100 );
				inProgress = false;
				for ( DoAddClasses runnable : runnables ) {
					inProgress = inProgress || runnable.isFailure() == null;
				}
			} while ( inProgress );

			for ( DoAddClasses runnable : runnables ) {
				assertNotNull( "Threads not run # " + runnable.getWorkNumber(), runnable.isFailure() );
				assertFalse( "thread failed #" + runnable.getWorkNumber() + " Failure: " + runnable.getFailureInfo(), runnable.isFailure() );
			}

			poolExecutor.awaitTermination( 1, TimeUnit.MINUTES );

			for ( int i = 0; i < nbrOfThread * nbrOfClassesPerThread; i++ ) {
				Query luceneQuery = parser.parse( "Emmanuel" + i );
				final Class<?> classByNumber = getClassByNumber( i, sf.getServiceManager() );
				IndexReader indexReader = sf.getIndexReaderAccessor().open( classByNumber );
				IndexSearcher searcher = new IndexSearcher( indexReader );
				TopDocs hits = searcher.search( luceneQuery, 1000 );
				assertEquals( 1, hits.totalHits );
				sf.getIndexReaderAccessor().close( indexReader );
			}
		}
	}

	private static Class<?> getClassByNumber(int i, ServiceManager serviceManager) throws ClassNotFoundException {
		ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
		Class<?> clazz;
		try {
			clazz = classLoaderService.classForName(
					Generated.A0.class.getName().replace( "A0", "A" + i )
			);
		}
		finally {
			serviceManager.releaseService( ClassLoaderService.class );
		}
		return clazz;
	}

	private static class DoAddClasses implements Runnable {
		private final ExtendedSearchIntegrator extendedIntegrator;
		private final int factorOfClassesPerThread;
		private final QueryParser parser;
		private final int nbrOfClassesPerThread;
		private volatile Boolean failure = false;
		private volatile String failureInfo;

		public String getFailureInfo() {
			return failureInfo;
		}

		public Boolean isFailure() {
			return failure;
		}

		public int getWorkNumber() {
			return factorOfClassesPerThread;
		}

		public DoAddClasses(SearchIntegrator si, int factorOfClassesPerThread, int nbrOfClassesPerThread) {
			this.extendedIntegrator = si.unwrap( ExtendedSearchIntegrator.class );
			this.factorOfClassesPerThread = factorOfClassesPerThread;
			this.parser = new QueryParser(
					TestConstants.getTargetLuceneVersion(),
					"name",
					TestConstants.standardAnalyzer
			);
			this.nbrOfClassesPerThread = nbrOfClassesPerThread;
		}

		@Override
		public void run() {
			try {
				for ( int index = 0; index < 10; index++ ) {
					final int i = factorOfClassesPerThread * nbrOfClassesPerThread + index;
					final Class<?> aClass = MutableFactoryTest.getClassByNumber(
							i,
							extendedIntegrator.getServiceManager()
					);
					extendedIntegrator.addClasses( aClass );
					Object entity = aClass.getConstructor( Integer.class, String.class )
							.newInstance( i, "Emmanuel" + i );
					TransactionContextForTest context = new TransactionContextForTest();
					MutableFactoryTest.doIndexWork( entity, i, extendedIntegrator, context );
					context.end();

					EntityIndexBinding indexBindingForEntity = extendedIntegrator.getIndexBinding( aClass );
					assertNotNull( indexBindingForEntity );
					IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
					assertEquals( 1, indexManagers.length );
					DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
					DirectoryProvider directoryProvider = indexManager.getDirectoryProvider();

					if ( !( directoryProvider instanceof RAMDirectoryProvider ) ) {
						// can't use Assertion in a separate thread
						throw new SearchException( "Configuration lost: expected RAM directory" );
					}

					Query luceneQuery = parser.parse( "Emmanuel" + i );
					IndexReader indexReader = extendedIntegrator.getIndexReaderAccessor().open( aClass );
					IndexSearcher searcher = new IndexSearcher( indexReader );
					TopDocs hits = searcher.search( luceneQuery, 1000 );
					if ( hits.totalHits != 1 ) {
						failure = true;
						failureInfo = "failure: Emmanuel" + i + " for " + aClass.getName();
						return;
					}
					extendedIntegrator.getIndexReaderAccessor().close( indexReader );
				}
			}
			catch (Exception e) {
				this.failure = true;
				e.printStackTrace();
				failureInfo = "failure: Emmanuel" + factorOfClassesPerThread + " exception: " + e.toString();
			}
		}
	}

}
