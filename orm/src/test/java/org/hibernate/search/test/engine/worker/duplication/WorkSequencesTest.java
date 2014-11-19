/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.worker.duplication;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import org.hibernate.Session;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.session.Domain;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Testcase for HSEARCH-353
 * Verify that different kinds of work (add/delete) found in the same
 * queue are all executed; having special care about different entities
 * being deleted/persisted but sharing the same PK (to replace the old
 * instance with another one).
 *
 * @author Sanne Grinovero
 */
public class WorkSequencesTest extends SearchTestBase {

	private SearchFactory searchFactory;

	@Test
	public void testComplexTransactionSequence() throws IOException {
		Session classicSession = openSession( );
		FullTextSession session = Search.getFullTextSession( classicSession );
		searchFactory = session.getSearchFactory();

		// create some different domains:
		{
			session.beginTransaction();
			session.persist( new Domain( 1, "jboss.org" ) );
			session.persist( new Domain( 2, "jboss.com" ) );
			session.persist( new Domain( 3, "hibernate.org" ) );
			session.persist( new Domain( 4, "geocities.com" ) );
			session.getTransaction().commit();
		}
		assertEquals( 2, countDomainsByFullText( "jboss" ) );
		assertEquals( 1, countDomainsByFullText( "hibernate" ) );
		assertEquals( 1, countDomainsByFullText( "geocities" ) );

		// now create some and delete others:
		{
			session.beginTransaction();
			session.persist( new Domain( 5, "sun.com" ) );
			session.persist( new Domain( 6, "mysql.com" ) );
			session.persist( new Domain( 7, "oracle.com" ) );
			Domain hibernateDomain = (Domain) session.get( Domain.class, 3 );
			session.delete( hibernateDomain );
			Domain geocitiesDomain = (Domain) session.get( Domain.class, 4 );
			session.delete( geocitiesDomain );
			session.getTransaction().commit();
		}
		assertEquals( 0, countDomainsByFullText( "hibernate" ) );
		assertEquals( 0, countDomainsByFullText( "geocities" ) );
		assertEquals( 2, countDomainsByFullText( "jboss" ) );
		assertEquals( 1, countDomainsByFullText( "sun" ) );
		assertEquals( 1, countDomainsByFullText( "mysql" ) );
		assertEquals( 1, countDomainsByFullText( "oracle" ) );

		// use create/update/delete:
		{
			session.beginTransaction();
			session.persist( new Domain( 3, "hibernate.org" ) );
			Domain mysqlDomain = (Domain) session.get( Domain.class, 6 );
			session.delete( mysqlDomain );
			//persisting a new entity having the same PK as a deleted one:
			session.persist( new Domain( 6, "myhql.org" ) );
			Domain sunDomain = (Domain) session.get( Domain.class, 5 );
			sunDomain.setName( "community.oracle.com" );
			session.getTransaction().commit();
		}
		assertEquals( 1, countDomainsByFullText( "hibernate" ) );
		assertEquals( 2, countDomainsByFullText( "oracle" ) );
		assertEquals( 1, countDomainsByFullText( "myhql" ) );
		assertEquals( 1, countDomainsByFullText( "community" ) );
		assertEquals( 0, countDomainsByFullText( "mysql" ) );

		// now creating and deleting the "same" (as by pk) entity several times in same transaction:
		{
			session.beginTransaction();
			session.persist( new Domain( 8, "mysql.org" ) );
			Domain mysqlDomain = (Domain) session.load( Domain.class, 8 );
			session.delete( mysqlDomain );
			Domain newDomain = new Domain( 8, "something.org" );
			session.persist( newDomain );
			session.delete( newDomain );
			session.persist( new Domain( 8, "somethingnew.org" ) );
			session.getTransaction().commit();
		}
		assertEquals( 1, countDomainsByFullText( "somethingnew" ) );

		session.close();
	}

	//helper method to verify how many instances are found in the index by doing a simple FT query
	private int countDomainsByFullText(String name) throws IOException {
		Query luceneQuery = new TermQuery( new Term( "name", name ) );
		IndexReader indexReader = searchFactory.getIndexReaderAccessor().open( Domain.class );
		try {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			TopDocs topDocs = searcher.search( luceneQuery, null, 100 );
			return topDocs.totalHits;
		}
		finally {
			searchFactory.getIndexReaderAccessor().close( indexReader );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Domain.class
		};
	}

}
