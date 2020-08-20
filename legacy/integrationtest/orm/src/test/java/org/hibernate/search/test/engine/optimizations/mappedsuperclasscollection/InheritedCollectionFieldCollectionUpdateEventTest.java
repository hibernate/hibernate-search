/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.optimizations.mappedsuperclasscollection;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verify that updates to collections defined in a @MappedSuperclass actually trigger a re-indexing.
 */
@TestForIssue(jiraKey = "HSEARCH-1583")
public class InheritedCollectionFieldCollectionUpdateEventTest extends SearchTestBase {
	private static final String FIRST_COLLECTION_VALUE = "1";
	private static final String SECOND_COLLECTION_VALUE = "2";

	private FullTextSession fullTextSession;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
	}

	@After
	@Override
	public void tearDown() throws Exception {
		fullTextSession.close();
		super.tearDown();
	}

	@Test
	public void testUpdateOfCollectionInMappedSuperclass() {
		EntityExtendingMappedSuperclassWithCollectionField entity = new EntityExtendingMappedSuperclassWithCollectionField();
		addToCollectionAndPersist( entity, FIRST_COLLECTION_VALUE );
		assertEquals(
				"First collection value is persisted and should be indexed",
				1,
				searchEntityByCollectionValue( FIRST_COLLECTION_VALUE ).size()
		);
		assertEquals(
				"Second collection value is not yet added and should not be indexed",
				0,
				searchEntityByCollectionValue( SECOND_COLLECTION_VALUE ).size()
		);
		fullTextSession.clear();

		// re-get the entity
		entity = (EntityExtendingMappedSuperclassWithCollectionField) fullTextSession.get(
				EntityExtendingMappedSuperclassWithCollectionField.class, entity.getId()
		);
		addToCollectionAndPersist( entity, SECOND_COLLECTION_VALUE );
		assertEquals(
				"Second collection value is persisted and should be indexed",
				1,
				searchEntityByCollectionValue( SECOND_COLLECTION_VALUE ).size()
		);
	}

	private void addToCollectionAndPersist(EntityExtendingMappedSuperclassWithCollectionField entity, String value) {
		Transaction transaction = fullTextSession.beginTransaction();
		entity.getCollection().add( value );
		fullTextSession.persist( entity );
		transaction.commit();
		fullTextSession.clear();
	}

	@SuppressWarnings("unchecked")
	private List<EntityExtendingMappedSuperclassWithCollectionField> searchEntityByCollectionValue(String value) {
		Transaction transaction = fullTextSession.beginTransaction();
		FullTextQuery query = fullTextSession.createFullTextQuery(
				new TermQuery( new Term( "collection", value ) ),
				EntityExtendingMappedSuperclassWithCollectionField.class
		);
		List<EntityExtendingMappedSuperclassWithCollectionField> result = query.list();
		transaction.commit();
		fullTextSession.clear();
		return result;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MappedSuperclassWithCollectionField.class,
				EntityExtendingMappedSuperclassWithCollectionField.class
		};
	}
}
