/*
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

package org.hibernate.search.test.engine.optimizations;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Test;

/**
 * Verify that updates to collections defined in a @MappedSuperclass actually trigger a re-indexing.
 */
@TestForIssue(jiraKey = "HSEARCH-1583")
public class InheritedCollectionFieldCollectionUpdateEventTest extends SearchTestCase {

	@Test
	public void test() {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		try {
			Transaction transaction = session.beginTransaction();

			EntityExtendingMappedSupperclassWithCollectionField entity = new EntityExtendingMappedSupperclassWithCollectionField();
			entity.getCollection().add( "1" );
			session.persist( entity );
			transaction.commit();
			session.clear();

			assertEquals( 1, searchByItem( fullTextSession, "1" ).size() );
			session.clear();

			transaction = session.beginTransaction();

			entity = (EntityExtendingMappedSupperclassWithCollectionField) session.get(
					EntityExtendingMappedSupperclassWithCollectionField.class, entity.getId() );
			entity.getCollection().add( "2" );
			session.save( entity );
			transaction.commit();
			session.clear();

			assertEquals( 1, searchByItem( fullTextSession, "2" ).size() );
		}
		finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	private List<EntityExtendingMappedSupperclassWithCollectionField> searchByItem(FullTextSession fullTextSession,
			String itemValue) {
		FullTextQuery query = fullTextSession.createFullTextQuery(
				new TermQuery( new Term( "collection", itemValue ) ),
				EntityExtendingMappedSupperclassWithCollectionField.class );
		return query.list();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MappedSuperclassWithCollectionField.class,
				EntityExtendingMappedSupperclassWithCollectionField.class };
	}

}
