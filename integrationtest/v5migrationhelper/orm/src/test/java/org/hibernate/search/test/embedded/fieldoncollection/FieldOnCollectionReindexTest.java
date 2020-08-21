/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.fieldoncollection;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FieldOnCollectionReindexTest extends SearchTestBase {

	@TestForIssue(jiraKey = "HSEARCH-1020")
	@Test
	public void testUpdatingCollectionWithFieldAnnotationReindexesEntity() {
		Session hibernateSession = openSession();

		IndexedEntity indexedEntity = new IndexedEntity( "child" );

		CollectionItem item1 = new CollectionItem();
		CollectionItem item2 = new CollectionItem();
		CollectionItem item3 = new CollectionItem();

		Transaction tx = hibernateSession.beginTransaction();
		hibernateSession.save( item1 );
		hibernateSession.save( item2 );
		hibernateSession.save( item3 );
		tx.commit();

		List<CollectionItem> items = new ArrayList<CollectionItem>();
		items.add( item1 );
		items.add( item3 );

		indexedEntity.setItemsWithFieldAnnotation( items );

		tx = hibernateSession.beginTransaction();
		hibernateSession.save( indexedEntity );
		tx.commit();

		List<IndexedEntity> searchResult;

		// Check that everything got indexed correctly
		tx = hibernateSession.beginTransaction();

		searchResult = searchIndexedEntity( hibernateSession, "itemsWithFieldAnnotation", item1 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		searchResult = searchIndexedEntity( hibernateSession, "itemsWithFieldAnnotation", item2 );
		assertEquals( 0, searchResult.size() );

		searchResult = searchIndexedEntity( hibernateSession, "itemsWithFieldAnnotation", item3 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		tx.commit();

		// Update the collection of the entity and its name
		items.clear();
		items.add( item2 );

		indexedEntity.setItemsWithFieldAnnotation( items );
		indexedEntity.setName( "new name" );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		// Everything is OK: the index is correctly updated
		searchResult = searchIndexedEntity( hibernateSession, "itemsWithFieldAnnotation", item2 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		// Now, let's update only the collection
		items.clear();
		items.add( item3 );

		indexedEntity.setItemsWithFieldAnnotation( items );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		tx = hibernateSession.beginTransaction();

		// The collection hasn't been indexed correctly
		// The following tests fail
		searchResult = searchIndexedEntity( hibernateSession, "itemsWithFieldAnnotation", item3 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		tx.commit();
		hibernateSession.close();
	}

	// Same test with @Fields annotation
	@TestForIssue(jiraKey = "HSEARCH-1020")
	@Test
	public void testUpdatingCollectionWithFieldsAnnotationReindexesEntity() {
		Session hibernateSession = openSession();

		IndexedEntity indexedEntity = new IndexedEntity( "child" );

		CollectionItem item1 = new CollectionItem();
		CollectionItem item2 = new CollectionItem();
		CollectionItem item3 = new CollectionItem();

		List<CollectionItem> items = new ArrayList<CollectionItem>();
		items.add( item1 );
		items.add( item3 );

		indexedEntity.setItemsWithFieldsAnnotation( items );

		// Saving entities
		Transaction tx = hibernateSession.beginTransaction();
		hibernateSession.save( item1 );
		hibernateSession.save( item2 );
		hibernateSession.save( item3 );
		hibernateSession.save( indexedEntity );
		tx.commit();

		List<IndexedEntity> searchResult;

		// Check that everything got indexed correctly
		tx = hibernateSession.beginTransaction();

		searchResult = searchIndexedEntity( hibernateSession, IndexedEntity.FIELD1_FIELD_NAME, item1 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		searchResult = searchIndexedEntity( hibernateSession, IndexedEntity.FIELD1_FIELD_NAME, item2 );
		assertEquals( 0, searchResult.size() );

		searchResult = searchIndexedEntity( hibernateSession, IndexedEntity.FIELD1_FIELD_NAME, item3 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		tx.commit();

		// Update the collection of the entity and its name
		items.clear();
		items.add( item2 );

		indexedEntity.setItemsWithFieldsAnnotation( items );
		indexedEntity.setName( "new name" );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		// Everything is OK: the index is correctly updated
		searchResult = searchIndexedEntity( hibernateSession, IndexedEntity.FIELD1_FIELD_NAME, item2 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		// Now, let's update only the collection
		items.clear();
		items.add( item3 );

		indexedEntity.setItemsWithFieldsAnnotation( items );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		tx = hibernateSession.beginTransaction();

		// The collection hasn't been indexed correctly
		// The following tests fail
		searchResult = searchIndexedEntity( hibernateSession, IndexedEntity.FIELD1_FIELD_NAME, item3 );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		tx.commit();
		hibernateSession.close();
	}

	@TestForIssue(jiraKey = "HSEARCH-1004")
	@Test
	public void testUpdatingElementCollectionWithFieldAnnotationReindexesEntity() {
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
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test2" );
		assertEquals( 0, searchResult.size() );

		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test3" );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		tx.commit();

		// Update the collection of the entity and its name
		indexedEntity.addKeyword( "test4" );
		indexedEntity.setName( "new name" );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		// Everything is OK: the index is correctly updated
		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test4" );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );

		// Now, let's update only the collection
		indexedEntity.addKeyword( "test5" );

		tx = hibernateSession.beginTransaction();
		hibernateSession.update( indexedEntity );
		tx.commit();

		tx = hibernateSession.beginTransaction();
		// The collection hasn't been indexed correctly
		// The following tests fail
		searchResult = searchIndexedEntity( hibernateSession, "keywords", "test5" );
		assertEquals( 1, searchResult.size() );
		assertEquals( searchResult.iterator().next().getId(), indexedEntity.getId() );
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
		return new Class[] { IndexedEntity.class, CollectionItem.class };
	}

}
