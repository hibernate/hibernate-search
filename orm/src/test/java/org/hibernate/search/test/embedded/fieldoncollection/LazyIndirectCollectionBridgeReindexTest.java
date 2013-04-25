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
package org.hibernate.search.test.embedded.fieldoncollection;

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
import org.hibernate.search.test.embedded.depth.Person;
import org.hibernate.search.test.util.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-1030")
public class LazyIndirectCollectionBridgeReindexTest extends SearchTestCase {

	public void testLazyIndirectCollectionBridgeReindex() throws InterruptedException {
		prepareEntities();
		verifyMatchExistsWithName( "name", "name" );

		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		MassIndexer massIndexer = fullTextSession.createIndexer( Root.class );
		massIndexer.startAndWait();
		verifyMatchExistsWithName( "name", "name" );
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
			List<Person> list = fullTextQuery.list();
			assertEquals( 1, list.size() );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	private void prepareEntities() {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();

			CollectionItem bridgedEntity = new CollectionItem();
			session.save( bridgedEntity );

			Leaf leaf = new Leaf();
			leaf.getCollectionItems().add( bridgedEntity );
			session.save( leaf );

			Root root = new Root();
			root.setName( "name" );
			root.setLeaf( leaf );
			session.save( root );

			transaction.commit();
		}
		finally {
			session.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CollectionItem.class, Leaf.class, Root.class };
	}

}
