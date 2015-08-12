/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.criteria;

import java.util.List;

import org.apache.lucene.search.Query;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Julie Ingignoli
 * @author Emmanuel Bernard
 */
public class ResultSizeOnCriteriaTest extends SearchTestBase {

	@Test
	public void testResultSize() {
		indexTestData();

		// Search
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		//Write query
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Tractor.class ).get();
		Query query = qb.keyword().wildcard().onField( "owner" ).matching( "p*" ).createQuery();


		//set criteria
		Criteria criteria = session.createCriteria( Tractor.class );
		criteria.add( Restrictions.eq( "hasColor", Boolean.FALSE ) );

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Tractor.class )
				.setCriteriaQuery( criteria );
		List<Tractor> result = hibQuery.list();
		//Result size is ok
		assertEquals( 1, result.size() );

		for ( Tractor tractor : result ) {
			assertThat( tractor.isHasColor() ).isFalse();
			assertThat( tractor.getOwner() ).startsWith( "P" );
		}

		//Compare with resultSize
		try {
			hibQuery.getResultSize();
			assertThat( true ).as( "HSEARCH000105 should have been raised" ).isFalse();
		}
		catch (SearchException e) {
			assertThat( e.getMessage() ).startsWith( "HSEARCH000105" );
		}
		assertThat( result ).hasSize( 1 );
		//getResultSize get only count of tractors matching keyword on field "owner" beginning with "p*"
		tx.commit();

		tx = session.beginTransaction();
		for ( Object element : session.createQuery( "select t from Tractor t" ).list() ) {
			session.delete( element );
		}
		tx.commit();
		session.close();

	}


	private void indexTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Tractor tractor = new Tractor();
		tractor.setKurztext( "tractor" );
		tractor.setOwner( "Paul" );
		tractor.removeColor();
		s.persist( tractor );

		Tractor tractor2 = new Tractor();
		tractor2.setKurztext( "tractor" );
		tractor2.setOwner( "Pierre" );
		s.persist( tractor2 );

		Tractor tractor3 = new Tractor();
		tractor3.setKurztext( "tractor" );
		tractor3.setOwner( "Jacques" );
		s.persist( tractor3 );

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Tractor.class
		};
	}
}
