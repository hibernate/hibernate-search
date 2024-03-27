/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

class AvoidDuplicatesTest extends SearchTestBase {

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Nation italy = new Nation( "Italy", "IT" );
		session.persist( italy );

		AncientBook aeneid = new AncientBook();
		aeneid.setTitle( "Aeneid" );
		aeneid.getAlternativeTitles().add( "Aeneis" );
		aeneid.getAlternativeTitles().add( "Eneide" );
		aeneid.setFirstPublishedIn( italy );
		session.persist( aeneid );

		AncientBook commedia = new AncientBook();
		commedia.setTitle( "Commedia" );
		commedia.getAlternativeTitles().add( "La Commedia" );
		commedia.getAlternativeTitles().add( "La Divina Commedia" );
		commedia.setFirstPublishedIn( italy );
		session.persist( commedia );

		transaction.commit();
		session.close();
	}

	@Test
	void testReindexedOnce() throws InterruptedException {
		assertThat( countBooksInIndex() ).isEqualTo( 2 );
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		MassIndexer massIndexer = fullTextSession.createIndexer();
		massIndexer.startAndWait();
		session.close();
		assertThat( countBooksInIndex() ).isEqualTo( 2 );
	}

	private int countBooksInIndex() {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		fullTextSession.beginTransaction();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		int size = fullTextQuery.list().size();
		fullTextSession.getTransaction().commit();
		fullTextSession.close();
		return size;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AncientBook.class,
				Book.class,
				Nation.class
		};
	}

}
