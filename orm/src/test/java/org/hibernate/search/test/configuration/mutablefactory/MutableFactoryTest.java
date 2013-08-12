/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration.mutablefactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;

import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.configuration.mutablefactory.generated.Generated;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class MutableFactoryTest {

	public static final Log log = LoggerFactory.make();

	@Test
	public void testCreateEmptyFactory() throws Exception {
		final ManualConfiguration configuration = getTestConfiguration();
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		sf.close();
	}

	@Test
	public void testAddingClassFullModel() throws Exception {
		ManualConfiguration configuration = getTestConfiguration();
		//FIXME downcasting of MSF. create a getDelegate() ?
		SearchFactoryIntegrator sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		final SearchFactoryBuilder builder = new SearchFactoryBuilder();
		sf = builder.currentFactory( sf )
				.addClass( A.class )
				.buildSearchFactory();

		ManualTransactionContext tc = new ManualTransactionContext();

		doIndexWork( new A(1, "Emmanuel"), 1, sf, tc );

		tc.end();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.standardAnalyzer );
		Query luceneQuery = parser.parse( "Emmanuel" );

		IndexReader indexReader = sf.getIndexReaderAccessor().open( A.class );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();
		sf.getIndexReaderAccessor().close( indexReader );

		sf = builder.currentFactory( sf )
				.addClass( B.class )
				.buildSearchFactory();

		tc = new ManualTransactionContext();

		doIndexWork( new B(1, "Noel"), 1, sf, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		indexReader = sf.getIndexReaderAccessor().open( B.class );
		searcher = new IndexSearcher( indexReader );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();
		sf.getIndexReaderAccessor().close( indexReader );

		sf.close();
	}

	@Test
	public void testAddingClassSimpleAPI() throws Exception {
		ManualConfiguration configuration = getTestConfiguration();
		SearchFactoryIntegrator sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();

		sf.addClasses( A.class );

		ManualTransactionContext tc = new ManualTransactionContext();

		doIndexWork( new A(1, "Emmanuel"), 1, sf, tc );

		tc.end();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.standardAnalyzer );
		Query luceneQuery = parser.parse( "Emmanuel" );

		IndexReader indexReader = sf.getIndexReaderAccessor().open( A.class );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();
		sf.getIndexReaderAccessor().close( indexReader );

		sf.addClasses( B.class, C.class );

		tc = new ManualTransactionContext();

		doIndexWork( new B(1, "Noel"), 1, sf, tc );
		doIndexWork( new C(1, "Vincent"), 1, sf, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		indexReader = sf.getIndexReaderAccessor().open( B.class );
		searcher = new IndexSearcher( indexReader );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );
		searcher.close();
		sf.getIndexReaderAccessor().close( indexReader );

		luceneQuery = parser.parse( "Vincent" );

		indexReader = sf.getIndexReaderAccessor().open( C.class );
		searcher = new IndexSearcher( indexReader );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();
		sf.getIndexReaderAccessor().close( indexReader );

		sf.close();
	}

	private static void doIndexWork(Object entity, Integer id, SearchFactoryIntegrator sfi, ManualTransactionContext tc) {
		Work<?> work = new Work<Object>( entity, id, WorkType.INDEX );
		sfi.getWorker().performWork( work, tc );
	}

	@Test
	public void testMultiThreadedAddClasses() throws Exception {
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.standardAnalyzer );
		ManualConfiguration configuration = getTestConfiguration();
		SearchFactoryIntegrator sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		List<DoAddClasses> runnables = new ArrayList<DoAddClasses>(10);
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
		} while (inProgress);

		for ( DoAddClasses runnable : runnables ) {
			assertNotNull( "Threads not run # " + runnable.getWorkNumber(), runnable.isFailure() );
			assertFalse( "thread failed #" + runnable.getWorkNumber() + " Failure: " + runnable.getFailureInfo(), runnable.isFailure() );
		}

		poolExecutor.awaitTermination( 1, TimeUnit.MINUTES );

		for ( int i = 0; i < nbrOfThread * nbrOfClassesPerThread; i++ ) {
			Query luceneQuery = parser.parse( "Emmanuel" + i);
			final Class<?> classByNumber = getClassAByNumber( i );
			IndexReader indexReader = sf.getIndexReaderAccessor().open( classByNumber );
			IndexSearcher searcher = new IndexSearcher( indexReader );
			TopDocs hits = searcher.search( luceneQuery, 1000 );
			assertEquals( 1, hits.totalHits );
			searcher.close();
			sf.getIndexReaderAccessor().close( indexReader );
		}
	}

	private static Class<?> getClassAByNumber(int i) throws ClassNotFoundException {
		final Class<?> aClass = ClassLoaderHelper.classForName(
				Generated.A0.class.getName().replace(
						"A0", "A" + i
				),
				Generated.A0.class.getClassLoader()
		);
		return aClass;
	}

	private static class DoAddClasses implements Runnable {
		private final SearchFactoryIntegrator factory;
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

		public DoAddClasses(SearchFactoryIntegrator factory, int factorOfClassesPerThread, int nbrOfClassesPerThread) {
			this.factory = factory;
			this.factorOfClassesPerThread = factorOfClassesPerThread;
			this.parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.standardAnalyzer );
			this.nbrOfClassesPerThread = nbrOfClassesPerThread;
		}

		@Override
		public void run() {

			try {
				for ( int index = 0 ; index < 10 ; index++ ) {
					final int i = factorOfClassesPerThread * nbrOfClassesPerThread + index;
					final Class<?> aClass = MutableFactoryTest.getClassAByNumber( i );
					factory.addClasses( aClass );
					Object entity = aClass.getConstructor( Integer.class, String.class ).newInstance( i, "Emmanuel" + i );
					ManualTransactionContext context = new ManualTransactionContext();
					MutableFactoryTest.doIndexWork( entity, i, factory, context );
					context.end();

					EntityIndexBinding indexBindingForEntity = factory.getIndexBinding( aClass );
					assertNotNull( indexBindingForEntity );
					IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
					assertEquals( 1, indexManagers.length );
					DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
					DirectoryProvider directoryProvider = indexManager.getDirectoryProvider();

					if ( ! ( directoryProvider instanceof RAMDirectoryProvider ) ) {
						// can't use Assertion in a separate thread
						throw new SearchException( "Configuration lost: expected RAM directory" );
					}

					Query luceneQuery = parser.parse( "Emmanuel" + i);
					IndexReader indexReader = factory.getIndexReaderAccessor().open( aClass );
					IndexSearcher searcher = new IndexSearcher( indexReader );
					TopDocs hits = searcher.search( luceneQuery, 1000 );
					if ( hits.totalHits != 1 ) {
						failure = true;
						failureInfo = "failure: Emmanuel" + i + " for " + aClass.getName();
						return;
					}
					searcher.close();
					factory.getIndexReaderAccessor().close( indexReader );
				}
			}
			catch (Exception e) {
				this.failure = true;
				e.printStackTrace();
				failureInfo = "failure: Emmanuel" + factorOfClassesPerThread + " exception: " + e.toString();
			}
		}
	}

	private static ManualConfiguration getTestConfiguration() {
		return new HibernateManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", "ram" )
			.addProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().name() );
	}

}
