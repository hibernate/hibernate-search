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
package org.hibernate.search.test.engine;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BusLine.class, BusStop.class };
	}

	// Test setup options - SessionFactory Properties
	@Override
	protected void configure(org.hibernate.cfg.Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( "hibernate.search.default.directory_provider", "ram" );
		configuration.setProperty( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}

}
