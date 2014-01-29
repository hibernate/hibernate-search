/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.jpa;

import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.TestConstants;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.index.Term;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class EntityManagerTest extends JPATestCase {

	@Test
	public void testMassIndexer() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		Bretzel bretzel = new Bretzel( 23, 34 );
		em.persist( bretzel );
		em.getTransaction().commit();
		em.clear();
		assertEquals( 1, countBretzelsViaIndex( em ) );
		em.purgeAll( Bretzel.class );
		em.flushToIndexes();
		assertEquals( 0, countBretzelsViaIndex( em ) );
		em.createIndexer( Bretzel.class ).startAndWait();
		assertEquals( 1, countBretzelsViaIndex( em ) );
	}

	private int countBretzelsViaIndex(FullTextEntityManager em) {
		QueryBuilder queryBuilder = em.getSearchFactory().buildQueryBuilder().forEntity( Bretzel.class ).get();
		Query allQuery = queryBuilder.all().createQuery();
		FullTextQuery fullTextQuery = em.createFullTextQuery( allQuery, Bretzel.class );
		return fullTextQuery.getResultSize();
	}

	@Test
	public void testQuery() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		Bretzel bretzel = new Bretzel( 23, 34 );
		em.persist( bretzel );
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();
		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );
		Query query = parser.parse( "saltQty:noword" );
		assertEquals( 0, em.createFullTextQuery( query ).getResultList().size() );
		query = new TermQuery( new Term("saltQty", "23.0") );
		assertEquals( "getResultList", 1, em.createFullTextQuery( query ).getResultList().size() );
		assertEquals( "getSingleResult and object retrieval", 23f,
				( (Bretzel) em.createFullTextQuery( query ).getSingleResult() ).getSaltQty(), 0f );
		assertEquals( 1, em.createFullTextQuery( query ).getResultSize() );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		em.remove( em.find( Bretzel.class, bretzel.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testIndex() {
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

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Bretzel.class
		};
	}
}
