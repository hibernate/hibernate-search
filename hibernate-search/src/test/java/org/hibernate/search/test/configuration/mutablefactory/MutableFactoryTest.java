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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import junit.framework.TestCase;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.slf4j.Logger;

import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.configuration.mutablefactory.generated.Generated;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class MutableFactoryTest extends TestCase {

	public static final Logger log = LoggerFactory.make();

	public void testCreateEmptyFactory() throws Exception {
		final ManualConfiguration configuration = new ManualConfiguration();
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		sf.close();
	}

	public void testAddingClassFullModel() throws Exception {
		ManualConfiguration configuration = new ManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		//FIXME downcasting of MSF. create a getDelegate() ?
		SearchFactoryIntegrator sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		final SearchFactoryBuilder builder = new SearchFactoryBuilder();
		sf = builder.currentFactory( sf )
				.addClass( A.class )
				.buildSearchFactory();

		ManualTransactionContext tc = new ManualTransactionContext();

		doIndexWork( new A(1, "Emmanuel"), 1, sf, tc );

		tc.end();

		QueryParser parser = new QueryParser( SearchTestCase.getTargetLuceneVersion(), "name", SearchTestCase.standardAnalyzer );
		Query luceneQuery = parser.parse( "Emmanuel" );

		//we know there is only one DP
		DirectoryProvider provider = sf
				.getDirectoryProviders( A.class )[0];
		IndexSearcher searcher = new IndexSearcher( provider.getDirectory(), true );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();

		sf = (MutableSearchFactory) builder.currentFactory( sf )
				.addClass( B.class )
				.buildSearchFactory();

		tc = new ManualTransactionContext();

		doIndexWork( new B(1, "Noel"), 1, sf, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		//we know there is only one DP
		provider = sf.getDirectoryProviders( B.class )[0];
		searcher = new IndexSearcher( provider.getDirectory(), true );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();

		sf.close();
	}

	public void testAddingClassSimpleAPI() throws Exception {
		ManualConfiguration configuration = new ManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		SearchFactoryIntegrator sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();

		sf.addClasses( A.class );

		ManualTransactionContext tc = new ManualTransactionContext();

		doIndexWork( new A(1, "Emmanuel"), 1, sf, tc );

		tc.end();

		QueryParser parser = new QueryParser( SearchTestCase.getTargetLuceneVersion(), "name", SearchTestCase.standardAnalyzer );
		Query luceneQuery = parser.parse( "Emmanuel" );

		//we know there is only one DP
		DirectoryProvider provider = sf
				.getDirectoryProviders( A.class )[0];
		IndexSearcher searcher = new IndexSearcher( provider.getDirectory(), true );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();

		sf.addClasses( B.class, C.class );

		tc = new ManualTransactionContext();

		doIndexWork( new B(1, "Noel"), 1, sf, tc );
		doIndexWork( new C(1, "Vincent"), 1, sf, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		//we know there is only one DP
		provider = sf.getDirectoryProviders( B.class )[0];
		searcher = new IndexSearcher( provider.getDirectory(), true );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		luceneQuery = parser.parse( "Vincent" );
		
		provider = sf.getDirectoryProviders( C.class )[0];
		searcher = new IndexSearcher( provider.getDirectory(), true );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();

		sf.close();
	}

	private static void doIndexWork(Object entity, Integer id, SearchFactoryIntegrator sfi, ManualTransactionContext tc) {
		Work<?> work = new Work<Object>( entity, id, WorkType.INDEX );
		sfi.getWorker().performWork( work, tc );
	}

	public void testMultiThreadedAddClasses() throws Exception {

		File indexDir = initIndexDirectory();
		try {
			doTestMultiThreadedClasses(indexDir);
		}
		finally {
			cleanIndexDir( indexDir );
		}
	}

	private void doTestMultiThreadedClasses(File indexDir) throws Exception {
		QueryParser parser = new QueryParser( SearchTestCase.getTargetLuceneVersion(), "name", SearchTestCase.standardAnalyzer );
		ManualConfiguration configuration = new ManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() )
				.addProperty( "hibernate.search.default.indexBase", indexDir.getAbsolutePath() );
		SearchFactoryIntegrator sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		List<DoAddClasses> runnables = new ArrayList<DoAddClasses>(10);
		final int nbrOfThread = 10;
		final int nbrOfClassesPerThread = 10;
		for (int i = 0 ; i < nbrOfThread; i++) {
			runnables.add( new DoAddClasses( sf, i, nbrOfClassesPerThread ) );
		}
		final ThreadPoolExecutor poolExecutor = Executors.newFixedThreadPool( nbrOfThread, "SFI classes addition" );
		poolExecutor.prestartAllCoreThreads();
		for (Runnable runnable : runnables) {
			poolExecutor.execute( runnable );
		}

		//poolExecutor.awaitTermination( 1, TimeUnit.MINUTES );
		boolean inProgress;
		do {
			Thread.sleep( 100 );
			inProgress = false;
			for ( DoAddClasses runnable : runnables) {
				inProgress = inProgress || runnable.isFailure() == null;
			}
		} while (inProgress);

		for ( DoAddClasses runnable : runnables) {
			assertNotNull( "Threads not run # " + runnable.getWorkNumber(), runnable.isFailure() );
			assertFalse( "thread failed #" + runnable.getWorkNumber() + " Failure: " + runnable.getFailureInfo(), runnable.isFailure() );
		}

		poolExecutor.shutdown();

		for (int i = 0 ; i < nbrOfThread*nbrOfClassesPerThread ; i++) {
			Query luceneQuery = parser.parse( "Emmanuel" + i);
			final Class<?> classByNumber = getClassAByNumber( i );
			DirectoryProvider<?> provider = sf.getDirectoryProviders( classByNumber )[0];
			IndexSearcher searcher = new IndexSearcher( provider.getDirectory(), true );
			TopDocs hits = searcher.search( luceneQuery, 1000 );
			assertEquals( 1, hits.totalHits );
		}
	}

	private void cleanIndexDir(File indexDir) {
		FileHelper.delete( indexDir );
	}

	private File initIndexDirectory() {
		String buildDir = System.getProperty( "build.dir" );
		if ( buildDir == null ) {
			buildDir = ".";
		}
		File current = new File( buildDir );
		File indexDir = new File( current, "indextemp" );
		boolean created = indexDir.mkdir();
		if (!created) {
			FileHelper.delete( indexDir );
			indexDir.mkdir();
		}
		return indexDir;
	}

	private static Class<?> getClassAByNumber(int i) throws ClassNotFoundException {
		final Class<?> aClass = ReflectHelper.classForName(
				Generated.A0.class.getName().replace(
						"A0", "A" + i
				)
		);
		return aClass;
	}

	private static class DoAddClasses implements Runnable {
		private final SearchFactoryIntegrator factory;
		private final int factorOfClassesPerThread;
		private final QueryParser parser;
		private final int nbrOfClassesPerThread;
		private volatile Boolean failure;
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
			this.parser = new QueryParser( SearchTestCase.getTargetLuceneVersion(), "name", SearchTestCase.standardAnalyzer );
			this.nbrOfClassesPerThread = nbrOfClassesPerThread;
		}

		public void run() {

			try {
				for (int index = 0 ; index < 10 ; index++) {
					final int i = factorOfClassesPerThread*nbrOfClassesPerThread + index;
					final Class<?> aClass = MutableFactoryTest.getClassAByNumber( i );
					factory.addClasses( aClass );
					Object entity = aClass.getConstructor( Integer.class, String.class ).newInstance(i, "Emmanuel" + i);
					ManualTransactionContext context = new ManualTransactionContext();
					MutableFactoryTest.doIndexWork(entity, i, factory, context );
					context.end();

					Query luceneQuery = parser.parse( "Emmanuel" + i);
					DirectoryProvider<?> provider = factory.getDirectoryProviders( aClass )[0];
					IndexSearcher searcher = new IndexSearcher( provider.getDirectory(), true );
					TopDocs hits = searcher.search( luceneQuery, 1000 );
					if ( hits.totalHits != 1 ) {
						failure = true;
						failureInfo = "failure: Emmanuel" + i + " for " + aClass.getName();
						return;
					}
					//System.out.println("success: Emmanuel" + i + " for " + aClass.getName() );
				}
				//System.out.println("success: Emmanuel" + factorOfClassesPerThread );
				failure = false;
			}
			catch ( Exception e ) {
				this.failure = true;
				e.printStackTrace(  );
				failureInfo = "failure: Emmanuel" + factorOfClassesPerThread + " exception: " + e.toString();
			}
		}
	}
}
