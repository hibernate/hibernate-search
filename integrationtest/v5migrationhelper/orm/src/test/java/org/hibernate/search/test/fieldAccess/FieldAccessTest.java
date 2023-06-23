/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.fieldAccess;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
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
public class FieldAccessTest extends SearchTestBase {

	@Test
	public void testFields() throws Exception {
		Document doc = new Document( "Hibernate in Action", "Object/relational mapping with Hibernate",
				"blah blah blah" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( doc );
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		tx = session.beginTransaction();
		QueryParser p = new QueryParser( "noDefaultField", TestConstants.standardAnalyzer );
		List result = session.createFullTextQuery( p.parse( "Abstract:Hibernate" ) ).list();
		assertEquals( "Query by field", 1, result.size() );
		s.delete( result.get( 0 ) );
		tx.commit();
		s.close();

	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}
}
