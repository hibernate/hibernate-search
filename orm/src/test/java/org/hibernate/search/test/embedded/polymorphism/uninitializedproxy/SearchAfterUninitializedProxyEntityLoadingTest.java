/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.polymorphism.uninitializedproxy;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HSEARCH-1448")
public class SearchAfterUninitializedProxyEntityLoadingTest extends SearchTestBase {
	private Integer entityId;
	private Integer entityReferenceId;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		populateDatabase();
	}

	@Test
	public void testSearchConcreteEntityWithoutPreLoadedProxy() {
		executeTest( ConcreteEntity.class, false );
	}

	@Test
	public void testSearchAbstractEntityWithoutPreLoadedProxy() {
		executeTest( AbstractEntity.class, false );
	}

	@Test
	public void testSearchConcreteEntityWithPreLoadedProxy() {
		executeTest( ConcreteEntity.class, true );
	}

	@Test
	public void testSearchAbstractEntityWithPreLoadedProxy() {
		executeTest( AbstractEntity.class, true );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AbstractEntity.class, ConcreteEntity.class, LazyAbstractEntityReference.class };
	}

	private void executeTest(Class<? extends AbstractEntity> clazz, boolean loadAbstractProxyBeforeSearch) {
		Session session = openSession();

		try {
			if ( loadAbstractProxyBeforeSearch ) {
				// Load a proxified version of the entity into the session
				LazyAbstractEntityReference reference = (LazyAbstractEntityReference) session.get(
						LazyAbstractEntityReference.class, entityReferenceId );
				assertTrue( reference != null && !Hibernate.isInitialized( reference.getEntity() ) );
			}

			// Search for the created entity
			assertEquals( 1, doSearch( session, clazz, entityId ).size() );
		}
		finally {
			session.close();
		}
	}

	private void populateDatabase() {
		Session session = openSession();

		try {
			Transaction t = session.beginTransaction();

			ConcreteEntity entity = new ConcreteEntity();
			session.save( entity );
			entityId = entity.getId();

			LazyAbstractEntityReference reference = new LazyAbstractEntityReference( entity );
			session.save( reference );
			entityReferenceId = reference.getId();

			session.flush();
			t.commit();
		}
		finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> doSearch(Session session, Class<T> clazz, Integer entityId) {
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		FullTextQuery query = fullTextSession.createFullTextQuery(
				new TermQuery( new Term( "id", entityId.toString() ) ), clazz );

		return query.list();
	}
}
