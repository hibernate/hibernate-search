// $Id$
package org.hibernate.search.test.similarity;

import java.util.List;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.Environment;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

/**
 * @author Emmanuel Bernard
 */
public class SimilarityTest extends SearchTestCase {
	public void testClassAndGlobalSimilarity() throws Exception {
	    Session s = openSession(  );
		Transaction tx = s.beginTransaction();
		Trash trash = new Trash();
		trash.setName( "Green trash" );
		s.persist( trash );
		trash = new Trash();
		trash.setName( "Green Green Green trash" );
		s.persist( trash );
		Can can = new Can();
		can.setName( "Green can" );
		s.persist( can );
		can = new Can();
		can.setName( "Green Green Green can" );
		s.persist( can );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		TermQuery tq = new TermQuery( new Term("name", "green") );
		FullTextSession fts = Search.getFullTextSession( s );
		List results = fts.createFullTextQuery( tq, Trash.class ).setProjection( FullTextQuery.SCORE, FullTextQuery.THIS ).list();
		assertEquals( 2, results.size() );
		assertEquals( "Similarity not overridden at the class level", ( (Object[]) results.get( 0 ) )[0], ( (Object[]) results.get( 1 ) )[0]);
		assertEquals( "Similarity not overridden", 1.0f, ( (Object[]) results.get( 0 ) )[0] );
		for (Object result : results) s.delete( ( (Object[]) result )[1] );

		results = fts.createFullTextQuery( tq, Can.class ).setProjection( FullTextQuery.SCORE, FullTextQuery.THIS ).list();
		assertEquals( 2, results.size() );
		assertEquals( "Similarity not overridden by the global setting", ( (Object[]) results.get( 0 ) )[0], ( (Object[]) results.get( 1 ) )[0]);
		assertFalse( "Similarity not overridden by the global setting", new Float(1.0f).equals( ( (Object[]) results.get( 0 ) )[0] ) );
		for (Object result : results) s.delete( ( (Object[]) result )[1] );

		tx.commit();
		s.close();

	}
	
	protected Class[] getMappings() {
		return new Class[] {
				Trash.class,
				Can.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.SIMILARITY_CLASS, DummySimilarity2.class.getName() );
		super.configure( cfg );
	}
}
