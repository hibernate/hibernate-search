/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.batchindexing;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.embedded.fieldbridgeonlazyfield.Root;
import org.hibernate.search.test.util.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-1240")
public class MassIndexerIndexedEmbeddedProxyTest extends SearchTestCase {

	private static final String TEST_NAME_CONTENT = "name";

	public void testMassIndexerWithProxyTest() throws InterruptedException {
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
			assertEquals( 1, resultSize );

			@SuppressWarnings("unchecked")
			List<Root> list = fullTextQuery.list();
			assertEquals( 1, list.size() );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IndexedEmbeddedProxyRootEntity.class, IndexedEmbeddedProxyLazyEntity.class };
	}

}
