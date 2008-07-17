//$Id$
package org.hibernate.search.test.jpa;

import org.hibernate.search.jpa.Search;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.Term;

/**
 * @author Emmanuel Bernard
 */
public class EntityManagerTest extends JPATestCase {

	public void testQuery() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		Bretzel bretzel = new Bretzel( 23, 34 );
		em.persist( bretzel );
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();
		QueryParser parser = new QueryParser( "title", new StopAnalyzer() );
		Query query = parser.parse( "saltQty:noword" );
		assertEquals( 0, em.createFullTextQuery( query ).getResultList().size() );
		query = new TermQuery( new Term("saltQty", "23.0") );
		assertEquals( "getResultList", 1, em.createFullTextQuery( query ).getResultList().size() );
		assertEquals( "getSingleResult and object retrieval", 23f,
				( (Bretzel)  em.createFullTextQuery( query ).getSingleResult() ).getSaltQty() );
		assertEquals( 1, em.createFullTextQuery( query ).getResultSize() );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		em.remove( em.find( Bretzel.class, bretzel.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	public void testIndex() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		Bretzel bretzel = new Bretzel( 23, 34 );
		em.persist( bretzel );
		em.getTransaction().commit();
		em.clear();

		//Not really a unit test but a test that shows the method call without failing
		//FIXME port the index test
		em.getTransaction().begin();
		em.index( em.find( Bretzel.class, bretzel.getId() ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		em.remove( em.find( Bretzel.class, bretzel.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Bretzel.class
		};
	}
}
