/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.session;

import static org.junit.Assert.assertEquals;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.Test;

import org.apache.lucene.queryparser.classic.QueryParser;

/**
 * @author Emmanuel Bernard
 */
public class OptimizeTest extends SearchTestBase {

	@Test
	public void testOptimize() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		int loop = 2000;
		for ( int i = 0; i < loop; i++ ) {
			Email email = new Email();
			email.setId( (long) i + 1 );
			email.setTitle( "JBoss World Berlin" );
			email.setBody( "Meet the guys who wrote the software" );
			s.persist( email );
		}
		tx.commit();
		s.close();

		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		s.getSearchFactory().optimize( Email.class );
		tx.commit();
		s.close();

		//check non indexed object get indexed by s.index
		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "noDefaultField", TestConstants.stopAnalyzer );
		int result = s.createFullTextQuery( parser.parse( "body:wrote" ) ).getResultSize();
		assertEquals( 2000, result );
		s.createQuery( "delete " + Email.class.getName() ).executeUpdate();
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Email.class,
				Domain.class
		};
	}
}
