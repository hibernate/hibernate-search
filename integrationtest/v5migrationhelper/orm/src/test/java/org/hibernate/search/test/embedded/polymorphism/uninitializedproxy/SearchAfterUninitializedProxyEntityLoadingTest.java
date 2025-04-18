/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.polymorphism.uninitializedproxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@TestForIssue(jiraKey = "HSEARCH-1448")
class SearchAfterUninitializedProxyEntityLoadingTest extends SearchTestBase {
	private Integer entityId;
	private Integer entityReferenceId;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		populateDatabase();
	}

	@Test
	void testSearchConcreteEntityWithoutPreLoadedProxy() {
		executeTest( ConcreteEntity.class, false );
	}

	@Test
	void testSearchAbstractEntityWithoutPreLoadedProxy() {
		executeTest( AbstractEntity.class, false );
	}

	@Test
	void testSearchConcreteEntityWithPreLoadedProxy() {
		executeTest( ConcreteEntity.class, true );
	}

	@Test
	void testSearchAbstractEntityWithPreLoadedProxy() {
		executeTest( AbstractEntity.class, true );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AbstractEntity.class, ConcreteEntity.class, LazyAbstractEntityReference.class };
	}

	private void executeTest(Class<? extends AbstractEntity> clazz, boolean loadAbstractProxyBeforeSearch) {
		try ( Session session = openSession() ) {
			if ( loadAbstractProxyBeforeSearch ) {
				// Load a proxified version of the entity into the session
				LazyAbstractEntityReference reference = (LazyAbstractEntityReference) session.get(
						LazyAbstractEntityReference.class, entityReferenceId );
				assertThat( reference != null && !Hibernate.isInitialized( reference.getEntity() ) ).isTrue();
			}

			// Search for the created entity
			assertThat( doSearch( session, clazz, entityId ) ).hasSize( 1 );
		}
	}

	private void populateDatabase() {
		try ( Session session = openSession(); ) {
			Transaction t = session.beginTransaction();

			ConcreteEntity entity = new ConcreteEntity();
			session.persist( entity );
			entityId = entity.getId();

			LazyAbstractEntityReference reference = new LazyAbstractEntityReference( entity );
			session.persist( reference );
			entityReferenceId = reference.getId();

			session.flush();
			t.commit();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> doSearch(Session session, Class<T> clazz, Integer entityId) {
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		Transaction tx = fullTextSession.beginTransaction();
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( clazz ).get();
		FullTextQuery query = fullTextSession.createFullTextQuery(
				qb.keyword().onField( "id" ).matching( entityId ).createQuery(), clazz );

		List<T> result = query.list();
		tx.commit();
		return result;
	}
}
