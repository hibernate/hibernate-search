/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.query.dsl.embedded;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Davide D'Alto
 */
public class DslEmbeddedSearchTest extends SearchTestCase {

	private Session s = null;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		EmbeddedEntity ee = new EmbeddedEntity();
		ee.setEmbeddedField( "embedded" );
		ee.setNumber( 7 );

		ContainerEntity pe = new ContainerEntity();
		pe.setEmbeddedEntity( ee );
		pe.setParentStringValue( "theparentvalue" );

		s = openSession();
		s.getTransaction().begin();
		s.persist( pe );
		s.getTransaction().commit();
	}

	@Override
	public void tearDown() throws Exception {
		s.clear();
		deleteAll( s, ContainerEntity.class );
		s.close();
		super.tearDown();
	}

	public void testSearchString() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ContainerEntity.class ).get();
		Query q = qb.keyword().onField( "emb.embeddedField" ).matching( "embedded" ).createQuery();
		List<ContainerEntity> results = execute( fullTextSession, q );

		assertEquals( "DSL didn't find the embedded string field", 1, results.size() );
		assertEquals( "embedded", results.get( 0 ).getEmbeddedEntity().getEmbeddedField() );

	}

	public void testSearchNumberWithFieldBridge() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ContainerEntity.class ).get();
		Query q = qb.keyword().onField( "emb.num" ).matching( 7 ).createQuery();
		List<ContainerEntity> results = execute( fullTextSession, q );

		assertEquals( "DSL didn't find the embedded numeric field", 1, results.size() );
		assertEquals( Integer.valueOf( 7 ), results.get( 0 ).getEmbeddedEntity().getNumber() );
	}

	@SuppressWarnings("unchecked")
	private List<ContainerEntity> execute(FullTextSession fullTextSession, Query q) {
		FullTextQuery combinedQuery = fullTextSession.createFullTextQuery( q, ContainerEntity.class );

		return combinedQuery.list();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ContainerEntity.class };
	}

	private void deleteAll(Session s, Class<?>... classes) {
		Transaction tx = s.beginTransaction();
		for ( Class<?> each : classes ) {
			List<?> list = s.createCriteria( each ).list();
			for ( Object object : list ) {
				s.delete( object );
			}
		}
		tx.commit();
	}
}
