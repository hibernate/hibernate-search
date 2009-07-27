// $Id$
package org.hibernate.search.test.shards;

import java.io.File;
import java.util.List;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.QueryParser;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.FileHelper;

/**
 * @author Chase Seibert
 */
public class DirectoryProviderForQueryTest extends SearchTestCase {


	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		// this strategy allows the caller to use a pre-search filter to define which index to hit	
		cfg.setProperty( "hibernate.search.Email.sharding_strategy", SpecificShardingStrategy.class.getCanonicalName() );		
		cfg.setProperty( "hibernate.search.Email.sharding_strategy.nbr_of_shards", "2" );
	}

	/**
	 * Test that you can filter by shard 
	 */
	public void testDirectoryProviderForQuery() throws Exception {
		
		Session s = openSession( );
		Transaction tx = s.beginTransaction();
		
		Email a = new Email();
		a.setId( 1 );
		a.setBody( "corporate message" );
		s.persist( a );
		
		a = new Email();
		a.setId( 2 );
		a.setBody( "spam message" );
		s.persist( a );
		
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser("id", new StopAnalyzer() );

		FullTextQuery fullTextQuery = fts.createFullTextQuery( parser.parse( "body:message" ) );
		List results = fullTextQuery.list();
		assertEquals( "Query with no filter should bring back results from both shards.", 2, results.size() );
		
		// index is not a field on the entity; the only way to filter on this is by shard
		fullTextQuery.enableFullTextFilter("shard").setParameter("index", 0);
		assertEquals( "Query with filter should bring back results from only one shard.", 1, fullTextQuery.list().size() );		
		
		for (Object o : results) s.delete( o );
		tx.commit();
		s.close();
	}

	protected void setUp() throws Exception {
		File sub = getBaseIndexDir();
		sub.mkdir();
		File[] files = sub.listFiles();
		for (File file : files) {
			if ( file.isDirectory() ) {
				FileHelper.delete( file );
			}
		}
		super.setUp(); //we need a fresh session factory each time for index set up
		buildSessionFactory( getMappings(), getAnnotatedPackages(), getXmlFiles() );
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		File sub = getBaseIndexDir();
		FileHelper.delete( sub );
	}

	@SuppressWarnings("unchecked")
	protected Class[] getMappings() {
		return new Class[] {
				Email.class
		};
	}
}
