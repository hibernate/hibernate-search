// $Id$
package org.hibernate.search.test.session;

import java.io.File;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.impl.FullTextSessionImpl;
import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.FileHelper;

/**
 * @author Emmanuel Bernard
 */
public class OptimizeTest extends SearchTestCase {

	public void testOptimize() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		int loop = 2000;
		for (int i = 0; i < loop; i++) {
			Email email = new Email();
			email.setId( (long) i + 1 );
			email.setTitle( "JBoss World Berlin" );
			email.setBody( "Meet the guys who wrote the software" );
			s.persist( email );
		}
		tx.commit();
		s.close();

		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		s.getSearchFactory().optimize( Email.class );
		tx.commit();
		s.close();

		//check non indexed object get indexed by s.index
		s = new FullTextSessionImpl( openSession() );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "id", new StopAnalyzer() );
		int result = s.createFullTextQuery( parser.parse( "body:wrote" ) ).getResultSize();
		assertEquals( 2000, result );
		s.createQuery( "delete " + Email.class.getName() ).executeUpdate();
		tx.commit();
		s.close();
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
		cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
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
		//super.setUp(); //we need a fresh session factory each time for index set up
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
				Email.class,
				Domain.class
		};
	}
}
