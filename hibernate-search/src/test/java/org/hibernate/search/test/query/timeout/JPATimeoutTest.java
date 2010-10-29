package org.hibernate.search.test.query.timeout;

import java.util.concurrent.TimeUnit;

import javax.persistence.QueryTimeoutException;

import org.apache.lucene.search.Query;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.jpa.JPATestCase;

/**
 * @author Emmanuel Bernard
 */
public class JPATimeoutTest extends JPATestCase {

	public void testQuerTimeoutException() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		for ( int i = 0 ; i < 1000 ; i++ ) {
			Clock clock  = new Clock("Model cat A" + i, (i % 2 == 0) ? "Seiko" : "Swatch", new Long (2000 + i) ) ;
			em.persist( clock );
		}
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query query = builder.keyword().onField( "brand" ).matching( "Seiko" ).createQuery();
		FullTextQuery hibernateQuery = em.createFullTextQuery( query, Clock.class );

		hibernateQuery.setHint( "javax.persistence.query.timeout", 1 );
		try {
			hibernateQuery.getResultSize();
			fail("timeout exception should happen");
		}
		catch ( QueryTimeoutException e ) {
			//good
			e.printStackTrace(  );
		}
		catch ( Exception e ) {
			fail("Expected a QueryTimeoutException");
		}
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		assertEquals( 1000, em.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
		em.getTransaction().commit();

		em.close();

	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}
}
