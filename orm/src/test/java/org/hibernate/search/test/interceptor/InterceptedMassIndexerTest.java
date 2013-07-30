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
package org.hibernate.search.test.interceptor;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Assert;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1190")
public class InterceptedMassIndexerTest extends SearchTestCase {

	public void testMassIndexerSkips() throws InterruptedException {
		storeSomeBlogs();
		assertIndexedBooks( 2 );
		rebuildIndexes();
		assertIndexedBooks( 2 );
	}

	/**
	 * Rebuild the index using a MassIndexer
	 *
	 * @throws InterruptedException
	 */
	private void rebuildIndexes() throws InterruptedException {
		Session session = openSession();
		try {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			fullTextSession.createIndexer( Blog.class ).startAndWait();
		}
		finally {
			session.close();
		}

	}

	/**
	 * Verify how many blogs we have in the index
	 *
	 * @param expectedBooks
	 */
	private void assertIndexedBooks(int expectedBooks) {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();
			try {
				FullTextSession fullTextSession = Search.getFullTextSession( session );
				Query allQuery = new MatchAllDocsQuery();
				FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( allQuery, Blog.class );
				int resultSize = fullTextQuery.getResultSize();
				Assert.assertEquals( expectedBooks, resultSize );
			}
			finally {
				transaction.commit();
			}
		}
		finally {
			session.close();
		}
	}

	/**
	 * Stores some test blogs: 2 published and a draft
	 */
	private void storeSomeBlogs() {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();
			try {

				Blog beta1 = new Blog();
				beta1.setTitle( "Hibernate Search 4.2.0.Beta1 is ready!!" );
				beta1.setStatus( BlogStatus.PUBLISHED );
				session.save( beta1 );

				Blog lucene4 = new Blog();
				lucene4.setTitle( "Apache Lucene 4 is ready. Now you can rewrite all your code from scratch!" );
				lucene4.setStatus( BlogStatus.PUBLISHED );
				session.save( lucene4 );

				Blog beta2 = new Blog();
				beta2.setTitle( "More Spatial, easy clustering, and JMX improvements" );
				beta2.setStatus( BlogStatus.DRAFT );
				session.save( beta2 );

			}
			finally {
				transaction.commit();
			}
		}
		finally {
			session.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Blog.class };
	}

}
