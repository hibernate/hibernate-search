//$Id$
package org.hibernate.search.test.query.criteria;

import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Hardy Ferentschik
 */
public class MixedCriteriaTest extends SearchTestCase {
	/**
	 * HSEARCH-360
	 */
	public void testCriteriaWithFilteredEntity() throws Exception {
		indexTestData();

		// Search
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		MultiFieldQueryParser parser = new MultiFieldQueryParser(
				new String[] { "kurztext" }, new StandardAnalyzer()
		);
		Query query = parser.parse( "combi OR sport" );

		Criteria criteria = session.createCriteria( AbstractCar.class );
		criteria.add( Restrictions.eq( "hasColor", Boolean.FALSE ) );

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( query, AbstractCar.class )
				.setCriteriaQuery( criteria );
		List result = hibQuery.list();
		assertEquals( 2, result.size() );
		tx.commit();
		session.close();
	}

	public void testCriteriaWithoutFilteredEntity() throws Exception {
		indexTestData();

		// Search
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		MultiFieldQueryParser parser = new MultiFieldQueryParser(
				new String[] { "kurztext" }, new StandardAnalyzer()
		);
		Query query = parser.parse( "combi OR sport" );

		Criteria criteria = session.createCriteria( AbstractCar.class );
		criteria.add( Restrictions.eq( "hasColor", Boolean.FALSE ) );

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( query )
				.setCriteriaQuery( criteria );
		List result = hibQuery.list();
		assertEquals( 2, result.size() );
		tx.commit();
		session.close();
	}

	public void testCriteriaWithMultipleEntities() throws Exception {
		indexTestData();

		// Search
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		MultiFieldQueryParser parser = new MultiFieldQueryParser(
				new String[] { "kurztext" }, new StandardAnalyzer()
		);
		Query query = parser.parse( "combi OR sport" );

		Criteria criteria = session.createCriteria( AbstractCar.class );
		criteria.add( Restrictions.eq( "hasColor", Boolean.FALSE ) );

		try {
			org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( query, AbstractCar.class, Bike.class )
					.setCriteriaQuery( criteria );
			hibQuery.list();
			fail();
		}
		catch ( SearchException se ) {
			assertEquals( "Cannot mix criteria and multiple entity types", se.getMessage() );
		}
		tx.commit();
		session.close();
	}

	private void indexTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		CombiCar combi = new CombiCar();
		combi.setKurztext( "combi" );
		s.persist( combi );

		SportCar sport = new SportCar();
		sport.setKurztext( "sport" );
		s.persist( sport );

		Bike bike = new Bike();
		bike.setKurztext( "bike" );
		s.persist( bike );
		tx.commit();
		s.close();
	}


	protected Class[] getMappings() {
		return new Class[] {
				AbstractCar.class, CombiCar.class, SportCar.class, Bike.class
		};
	}
}
