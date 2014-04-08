/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.engine.optimizations.mappedsuperclasscollection;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verify that updates to collections defined in a @MappedSuperclass actually trigger a re-indexing.
 */
@TestForIssue(jiraKey = "HSEARCH-1583")
public class InheritedCollectionFieldCollectionUpdateEventTest extends SearchTestCaseJUnit4 {
	private static final String FIRST_COLLECTION_VALUE = "1";
	private static final String SECOND_COLLECTION_VALUE = "2";

	private FullTextSession fullTextSession;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
	}

	@After
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
		FullTextQuery query = fullTextSession.createFullTextQuery(
				new TermQuery( new Term( "collection", value ) ),
				EntityExtendingMappedSuperclassWithCollectionField.class
		);
		return query.list();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MappedSuperclassWithCollectionField.class,
				EntityExtendingMappedSuperclassWithCollectionField.class
		};
	}
}
