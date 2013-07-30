/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
import org.hibernate.search.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Julie Ingignoli
 * @author Emmanuel Bernard <emmanuel@hibernate,org>
 */
public class ResultSizeOnCriteriaTest extends SearchTestCaseJUnit4 {

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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Tractor.class
		};
	}
}
