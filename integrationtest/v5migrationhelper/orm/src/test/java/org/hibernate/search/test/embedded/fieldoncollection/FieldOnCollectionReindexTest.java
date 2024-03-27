/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.fieldoncollection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

class FieldOnCollectionReindexTest extends SearchTestBase {

	@TestForIssue(jiraKey = "HSEARCH-1004")
	@Test
	void testUpdatingElementCollectionWithFieldAnnotationReindexesEntity() {
		Session hibernateSession = openSession();

		IndexedEntity indexedEntity = new IndexedEntity( "child" );
		indexedEntity.addKeyword( "test1" );
		indexedEntity.addKeyword( "test3" );

		// Saving entities
		Transaction tx = hibernateSession.beginTransaction();
		hibernateSession.save( indexedEntity );
		tx.commit();

		List<IndexedEntity> searchResult;

		// Check that everything got indexed correctly
		tx = hibernateSession.beginTransaction();

		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test1" );
		assertThat( searchResult ).hasSize( 1 );
		assertThat( searchResult.iterator().next().getId() ).isEqualTo( indexedEntity.getId() );

		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test2" );
		assertThat( searchResult ).isEmpty();

		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test3" );
		assertThat( searchResult ).hasSize( 1 );
		assertThat( searchResult.iterator().next().getId() ).isEqualTo( indexedEntity.getId() );

		tx.commit();

		// Update the collection of the entity and its name
		indexedEntity.addKeyword( "test4" );
		indexedEntity.setName( "new name" );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		// Everything is OK: the index is correctly updated
		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test4" );
		assertThat( searchResult ).hasSize( 1 );
		assertThat( searchResult.iterator().next().getId() ).isEqualTo( indexedEntity.getId() );

		// Now, let's update only the collection
		indexedEntity.addKeyword( "test5" );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		tx = hibernateSession.beginTransaction();
		// The collection hasn't been indexed correctly
		// The following tests fail
		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test5" );
		assertThat( searchResult ).hasSize( 1 );
		assertThat( searchResult.iterator().next().getId() ).isEqualTo( indexedEntity.getId() );
		tx.commit();

		hibernateSession.close();
	}

	@SuppressWarnings("unchecked")
	private List<IndexedEntity> searchIndexedEntity(Session session, String field, Object value) {
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( IndexedEntity.class ).get();

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
				queryBuilder.keyword().onField( field ).matching( value ).createQuery(),
				IndexedEntity.class );

		return (List<IndexedEntity>) fullTextQuery.list();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { IndexedEntity.class };
	}

}
