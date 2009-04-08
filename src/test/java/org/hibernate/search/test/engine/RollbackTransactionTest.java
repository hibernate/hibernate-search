// $Id$
package org.hibernate.search.test.engine;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;

/**
 * Verify index changes queued during a transaction are canceled
 * when the transaction is rolled back.
 * 
 * @author Sanne Grinovero
 */
public class RollbackTransactionTest extends SearchTestCase {
	
	public void testTransactionBehaviour() {
		assertEquals( 0, countBusLinesByFullText() );
		assertEquals( 0, countBusLineByDatabaseCount() );
		createBusLines( 5, true );
		assertEquals( 0, countBusLinesByFullText() );
		assertEquals( 0, countBusLineByDatabaseCount() );
		createBusLines( 5, false );
		assertEquals( 5, countBusLinesByFullText() );
		assertEquals( 5, countBusLineByDatabaseCount() );
		createBusLines( 7, true );
		assertEquals( 5, countBusLinesByFullText() );
		assertEquals( 5, countBusLineByDatabaseCount() );
		createBusLines( 7, false );
		assertEquals( 12, countBusLinesByFullText() );
		assertEquals( 12, countBusLineByDatabaseCount() );
	}
	
	private void createBusLines(int number, boolean rollback) {
		FullTextSession fullTextSession = Search.getFullTextSession( sessions.openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		for (int i=0; i<number; i++ ) {
			BusLine line = new BusLine();
			line.setBusLineName( "line " + i );
			fullTextSession.persist( line );
		}
		if ( rollback ) {
			tx.rollback();
		}
		else {
			tx.commit();
		}
		fullTextSession.close();
	}

	public int countBusLinesByFullText() {
		FullTextSession fullTextSession = Search.getFullTextSession( sessions.openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		org.apache.lucene.search.Query ftQuery = new MatchAllDocsQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( ftQuery, BusLine.class );
		int count = query.list().size();
		tx.commit();
		fullTextSession.close();
		return count;
	}
	
	public int countBusLineByDatabaseCount() {
		FullTextSession fullTextSession = Search.getFullTextSession( sessions.openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		int count = fullTextSession.createCriteria( BusLine.class ).list().size();
		tx.commit();
		fullTextSession.close();
		return count;
	}
	
	// Test setup options - Entities
	@Override
	protected Class[] getMappings() {
		return new Class[] { BusLine.class, BusStop.class };
	}
	
	// Test setup options - SessionFactory Properties
	@Override
	protected void configure(org.hibernate.cfg.Configuration configuration) {
		super.configure( configuration );
		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		cfg.setProperty( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}

}
