/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.prefixed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * @author Davide D'Alto
 */
class PrefixedEmbeddedCaseInPathTest extends SearchTestBase {

	private Session s = null;
	private EntityA entityA = null;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		EntityC indexedC = new EntityC( "indexed" );
		EntityC skippedC = new EntityC( "indexed" );

		EntityB indexedB = new EntityB( indexedC, skippedC );

		entityA = new EntityA( indexedB );
		s = openSession();
		persistEntity( s, indexedC, skippedC, indexedB, entityA );
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		s.clear();

		deleteAll( s, EntityA.class, EntityB.class, EntityC.class );
		s.close();
		super.tearDown();
	}

	@Test
	void testFieldIsIndexedIfInPath() {
		List<EntityA> result = search( s, "prefixed_idx_field", "indexed" );

		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ).id ).isEqualTo( entityA.id );
	}

	@Test
	void testFieldNotIndexedIfNotInPath() {
		assertThatThrownBy( () -> search( s, "prefixed_idx_skipped", "skipped" ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void testEmbeddedNotIndexedIfNotInPath() {
		assertThatThrownBy( () -> search( s, "prefixed_skp_field", "indexed" ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void testEmbeddedNotIndexedIfNotInPath2() {
		assertThatThrownBy( () -> search( s, "prefixed_skp_skipped", "skipped" ) )
				.isInstanceOf( SearchException.class );
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
			List<?> list = listAll( s, each );
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
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class, EntityC.class };
	}
}
