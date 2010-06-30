package org.hibernate.search.test.configuration.mutablefactory;

import junit.framework.TestCase;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import org.hibernate.search.IncrementalSearchFactory;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.impl.SearchFactoryBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.ManualTransactionContext;

/**
 * @author Emmanuel Bernard
 */
public class MutableFactoryTest extends TestCase {

	public void testCreateEmptyFactory() throws Exception {
		final ManualConfiguration configuration = new ManualConfiguration();
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		sf.close();
	}

	public void testAddingClassFullModel() throws Exception {
		ManualConfiguration configuration = new ManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		//FIXME downcasting of MSF. create a getDelegate() ?
		MutableSearchFactory sf = (MutableSearchFactory) new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		final SearchFactoryBuilder builder = new SearchFactoryBuilder();
		sf = (MutableSearchFactory) builder.rootFactory( sf )
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

		sf = (MutableSearchFactory) builder.rootFactory( sf )
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
		//FIXME downcasting of MSF. create a getDelegate() ?
		IncrementalSearchFactory sf = (IncrementalSearchFactory) new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		SearchFactoryImplementor sfi = (SearchFactoryImplementor) sf;
		sf.addClasses( A.class );

		ManualTransactionContext tc = new ManualTransactionContext();

		doIndexWork( new A(1, "Emmanuel"), 1, sfi, tc );

		tc.end();

		QueryParser parser = new QueryParser( SearchTestCase.getTargetLuceneVersion(), "name", SearchTestCase.standardAnalyzer );
		Query luceneQuery = parser.parse( "Emmanuel" );

		//we know there is only one DP
		DirectoryProvider provider = sfi
				.getDirectoryProviders( A.class )[0];
		IndexSearcher searcher = new IndexSearcher( provider.getDirectory(), true );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();

		sf.addClasses( B.class, C.class );

		tc = new ManualTransactionContext();

		doIndexWork( new B(1, "Noel"), 1, sfi, tc );
		doIndexWork( new C(1, "Vincent"), 1, sfi, tc );

		tc.end();

		luceneQuery = parser.parse( "Noel" );

		//we know there is only one DP
		provider = sfi.getDirectoryProviders( B.class )[0];
		searcher = new IndexSearcher( provider.getDirectory(), true );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		luceneQuery = parser.parse( "Vincent" );
		
		provider = sfi.getDirectoryProviders( C.class )[0];
		searcher = new IndexSearcher( provider.getDirectory(), true );
		hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 1, hits.totalHits );

		searcher.close();

		sfi.close();
	}

	private void doIndexWork(Object entity, Integer id, SearchFactoryImplementor sfi, ManualTransactionContext tc) {
		Work<?> work = new Work<Object>( entity, id, WorkType.INDEX );
		sfi.getWorker().performWork( work, tc );
	}
}
