/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.batchindexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

@TestForIssue(jiraKey = "HSEARCH-1240")
class MassIndexerIndexedEmbeddedProxyTest extends SearchTestBase {

	private static final String TEST_NAME_CONTENT = "name";

	@Test
	void testMassIndexerWithProxyTest() throws InterruptedException {
		prepareEntities();

		verifyMatchExistsWithName( "lazyEntity.name", TEST_NAME_CONTENT );
		verifyMatchExistsWithName( "lazyEntity2.name", TEST_NAME_CONTENT );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		MassIndexer massIndexer = fullTextSession.createIndexer( IndexedEmbeddedProxyRootEntity.class );
		massIndexer.startAndWait();
		fullTextSession.close();

		verifyMatchExistsWithName( "lazyEntity.name", TEST_NAME_CONTENT );
		verifyMatchExistsWithName( "lazyEntity2.name", TEST_NAME_CONTENT );
	}

	@Test
	void testMassIndexerRepeatedInvocation() throws InterruptedException {
		//Test that the MassIndexer can be started multiple times
		prepareEntities();

		verifyMatchExistsWithName( "lazyEntity.name", TEST_NAME_CONTENT );
		verifyMatchExistsWithName( "lazyEntity2.name", TEST_NAME_CONTENT );

		for ( int i = 0; i < 4; i++ ) {
			try ( FullTextSession fullTextSession = Search.getFullTextSession( openSession() ) ) {
				Transaction tx = fullTextSession.beginTransaction();
				fullTextSession.purgeAll( IndexedEmbeddedProxyRootEntity.class );
				tx.commit();
			}
			verifyIndexIsEmpty();

			try ( FullTextSession fullTextSession = Search.getFullTextSession( openSession() ) ) {
				MassIndexer massIndexer = fullTextSession.createIndexer( IndexedEmbeddedProxyRootEntity.class );
				massIndexer.startAndWait();
			}
			verifyMatchExistsWithName( "lazyEntity.name", TEST_NAME_CONTENT );
			verifyMatchExistsWithName( "lazyEntity2.name", TEST_NAME_CONTENT );
		}
	}

	private void verifyIndexIsEmpty() {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Query q = new MatchAllDocsQuery();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			int resultSize = fullTextQuery.getResultSize();
			List list = fullTextQuery.list();
			assertThat( resultSize ).isZero();
			assertThat( list ).isEmpty();
		}
		finally {
			fullTextSession.close();
		}
	}

	private void prepareEntities() {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();

			IndexedEmbeddedProxyLazyEntity lazyEntity = new IndexedEmbeddedProxyLazyEntity();
			lazyEntity.setName( TEST_NAME_CONTENT );
			session.save( lazyEntity );

			IndexedEmbeddedProxyRootEntity rootEntity = new IndexedEmbeddedProxyRootEntity();
			rootEntity.setLazyEntity( lazyEntity );
			rootEntity.setLazyEntity2( lazyEntity );
			session.save( rootEntity );

			transaction.commit();
		}
		finally {
			session.close();
		}
	}

	private void verifyMatchExistsWithName(String fieldName, String fieldValue) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new TermQuery( new Term( fieldName, fieldValue ) );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			int resultSize = fullTextQuery.getResultSize();
			assertThat( resultSize ).isEqualTo( 1 );

			@SuppressWarnings("unchecked")
			List<IndexedEmbeddedProxyRootEntity> list = fullTextQuery.list();
			assertThat( list ).hasSize( 1 );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IndexedEmbeddedProxyRootEntity.class, IndexedEmbeddedProxyLazyEntity.class };
	}

}
