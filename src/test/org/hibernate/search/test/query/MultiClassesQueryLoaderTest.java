// $Id$
package org.hibernate.search.test.query;

import java.sql.Statement;
import java.util.List;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public class MultiClassesQueryLoaderTest extends SearchTestCase {

	public void testObjectNotFound() throws Exception {
		Session sess = openSession();
		Transaction tx = sess.beginTransaction();
		Author author = new Author();
		author.setName( "Moo Cow" );
		sess.persist( author );

		tx.commit();
		sess.clear();
		Statement statement = sess.connection().createStatement();
		statement.executeUpdate( "DELETE FROM Author" );
		statement.close();
		FullTextSession s = Search.getFullTextSession( sess );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "title", new KeywordAnalyzer() );
		Query query = parser.parse( "name:moo" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Author.class, Music.class );
		List result = hibQuery.list();
		assertEquals( "Should have returned no author", 0, result.size() );

		for (Object o : s.createCriteria( Object.class ).list()) {
			s.delete( o );
		}

		tx.commit();
		s.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
				Author.class,
				Music.class
		};
	}
}
