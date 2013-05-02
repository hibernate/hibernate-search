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
package org.hibernate.search.test.batchindexing;

import junit.framework.Assert;
import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

public class AvoidDuplicatesTest extends SearchTestCase {

	@Override
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

	public void testReindexedOnce() throws InterruptedException {
		Assert.assertEquals( 2, countBooksInIndex() );
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		MassIndexer massIndexer = fullTextSession.createIndexer();
		massIndexer.startAndWait();
		session.close();
		Assert.assertEquals( 2, countBooksInIndex() );
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AncientBook.class,
				Book.class,
				Nation.class
		};
	}

}
