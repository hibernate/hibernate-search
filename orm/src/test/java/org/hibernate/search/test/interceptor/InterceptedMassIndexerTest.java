/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.interceptor;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1190")
public class InterceptedMassIndexerTest extends SearchTestBase {

	@Test
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
