//$Id$
package org.hibernate.search.test.query;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * HSEARCH-162 - trying to index an entity which is not marked with @Indexed
 *
 * @author Hardy Ferentschik
 */
public class QueryUnindexedEntityTest extends SearchTestCase {

	public void testQueryOnAllEntities() throws Exception {

		FullTextSession s = Search.getFullTextSession( openSession() );

		Transaction tx = s.beginTransaction();
		Person person = new Person();
		person.setName( "Jon Doe" );
		s.save( person );
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "name", new StandardAnalyzer() );
		Query query = parser.parse( "name:foo" );
		FullTextQuery hibQuery = s.createFullTextQuery( query );
		try {
			hibQuery.list();
			fail();
		}
		catch ( HibernateException e ) {
			assertTrue( "Wrong message", e.getMessage().startsWith( "There are no mapped entities" ) );
		}

		tx.rollback();
		s.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
				Person.class,
		};
	}
}