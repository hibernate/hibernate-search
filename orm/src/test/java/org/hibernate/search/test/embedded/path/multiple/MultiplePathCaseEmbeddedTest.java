/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.embedded.path.multiple;

import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Davide D'Alto
 */
public class MultiplePathCaseEmbeddedTest extends SearchTestCase {

	private Session s = null;
	private EntityA entityA = null;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		EntityC indexedC = new EntityC( "indexed" );
		indexedC.anotherField = "anotherField";
		EntityC skippedC = new EntityC( "indexed" );

		EntityB indexedB = new EntityB( indexedC, skippedC );

		entityA = new EntityA( indexedB );
		s = openSession();
		persistEntity( s, indexedC, skippedC, indexedB, entityA );
	}

	@Override
	public void tearDown() throws Exception {
		s.clear();

		deleteAll( s, EntityA.class, EntityB.class, EntityC.class );
		s.close();
		super.tearDown();
	}

	public void testRenamedFieldInFieldsIsIndexedIfInPath() throws Exception {
		List<EntityA> result = search( s, "b.indexed.renamed", "indexed" );

		Assert.assertEquals( 1, result.size() );
		Assert.assertEquals( entityA.id, result.get( 0 ).id );
	}

	public void testAnotherFieldIsIndexedIfInPath() throws Exception {
		List<EntityA> result = search( s, "b.indexed.anotherField", "anotherField" );

		Assert.assertEquals( 1, result.size() );
		Assert.assertEquals( entityA.id, result.get( 0 ).id );
	}

	public void testFieldNotIndexedIfInPathWithAttributeName() throws Exception {
		try {
			search( s, "b.indexed.field", "indexed" );
			fail( "Should not index embedded property if not in path and not in depth limit" );
		}
		catch (SearchException e) {
		}
	}

	public void testRenamedFieldNotIndexedIfInNotPath() throws Exception {
		try {
			search( s, "b.indexed.renamedSkipped", "indexed" );
			fail( "Should not index embedded property if not in path and not in depth limit" );
		}
		catch (SearchException e) {
		}
	}


	public void testEmbeddedNotIndexedIfNotInPath() throws Exception {
		try {
			search( s, "b.skipped.indexed", "indexed" );
			fail( "Should not index embedded property if not in path and not in depth limit" );
		}
		catch (SearchException e) {
		}
	}

	public void testFieldNotIndexedIfNotInPath() throws Exception {
		try {
			search( s, "b.indexed.skipped", "skipped" );
			fail( "Should not index embedded property if not in path and not in depth limit" );
		}
		catch (SearchException e) {
		}
	}

	private List<EntityA> search(Session s, String field, String value) {
		FullTextSession session = Search.getFullTextSession( s );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( EntityA.class ).get();
		Query query = queryBuilder.keyword().onField( field ).matching( value ).createQuery();
		@SuppressWarnings("unchecked")
		List<EntityA> result = session.createFullTextQuery( query ).list();
		return result;
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

	private void persistEntity(Session s, Object... entities) {
		Transaction tx = s.beginTransaction();
		for ( Object entity : entities ) {
			s.persist( entity );
		}
		tx.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class, EntityC.class };
	}
}
