/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.util.Map;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verify index changes queued during a transaction are canceled
 * when the transaction is rolled back.
 *
 * @author Sanne Grinovero
 */
public class RollbackTransactionTest extends SearchTestBase {

	@Test
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
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		for ( int i = 0; i < number; i++ ) {
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
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		org.apache.lucene.search.Query ftQuery = new MatchAllDocsQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( ftQuery, BusLine.class );
		int count = query.list().size();
		tx.commit();
		fullTextSession.close();
		return count;
	}

	public int countBusLineByDatabaseCount() {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		int count = fullTextSession.createCriteria( BusLine.class ).list().size();
		tx.commit();
		fullTextSession.close();
		return count;
	}

	// Test setup options - Entities
	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { BusLine.class, BusStop.class };
	}

	// Test setup options - SessionFactory Properties
	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.directory_provider", "local-heap" );
		cfg.put( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}

}
