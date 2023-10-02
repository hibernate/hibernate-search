/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * Verify index changes queued during a transaction are canceled
 * when the transaction is rolled back.
 *
 * @author Sanne Grinovero
 */
class RollbackTransactionTest extends SearchTestBase {

	@Test
	void testTransactionBehaviour() {
		assertThat( countBusLinesByFullText() ).isZero();
		assertThat( countBusLineByDatabaseCount() ).isZero();
		createBusLines( 5, true );
		assertThat( countBusLinesByFullText() ).isZero();
		assertThat( countBusLineByDatabaseCount() ).isZero();
		createBusLines( 5, false );
		assertThat( countBusLinesByFullText() ).isEqualTo( 5 );
		assertThat( countBusLineByDatabaseCount() ).isEqualTo( 5 );
		createBusLines( 7, true );
		assertThat( countBusLinesByFullText() ).isEqualTo( 5 );
		assertThat( countBusLineByDatabaseCount() ).isEqualTo( 5 );
		createBusLines( 7, false );
		assertThat( countBusLinesByFullText() ).isEqualTo( 12 );
		assertThat( countBusLineByDatabaseCount() ).isEqualTo( 12 );
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
		int count = listAll( fullTextSession, BusLine.class ).size();
		tx.commit();
		fullTextSession.close();
		return count;
	}

	// Test setup options - Entities
	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { BusLine.class, BusStop.class };
	}

}
