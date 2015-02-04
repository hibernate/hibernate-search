/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.interceptor;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-642")
public class ManualIndexingOnlyInterceptorTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private List<Foo> testEntities;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		createTestData();
	}

	@Test
	public void testAutomaticIndexUpdatesAreProhibitedByInterceptor() throws Exception {
		indexTestData();

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		assertEquals(
				"There should be no indexed entities, since automatic indexing is disabled via interceptor",
				0,
				fullTextQuery.list().size()
		);
	}

	@Test
	public void testExplicitIndexingIgnoresInterceptor() throws Exception {
		indexTestData();

		Transaction tx = fullTextSession.beginTransaction();
		for ( Foo foo : testEntities ) {
			Foo attachedFoo = (Foo) fullTextSession.merge( foo );
			fullTextSession.index( attachedFoo );
		}
		tx.commit();

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		assertEquals(
				"All test entities should be indexed",
				testEntities.size(),
				fullTextQuery.list().size()
		);
	}

	@Test
	public void testIndexUpdatesViaMassIndexerProhibitedByInterceptor() throws Exception {
		indexTestData();

		MassIndexer massIndexer = fullTextSession.createIndexer();
		massIndexer.startAndWait();

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		assertEquals(
				"There should be no indexed entities, since interceptor also applied for mass indexer",
				0,
				fullTextQuery.list().size()
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Foo.class
		};
	}

	private void createTestData() {
		testEntities = new ArrayList<>();
		for ( int i = 0; i < 3; i++ ) {
			testEntities.add( new Foo() );
		}
	}

	private void indexTestData() {
		Transaction tx = fullTextSession.beginTransaction();

		for ( Foo foo : testEntities ) {
			fullTextSession.save( foo );
		}

		tx.commit();
		fullTextSession.clear();
	}

	@Entity
	@Indexed(interceptor = ManualIndexingOnly.class)
	public static class Foo {
		@Id
		@GeneratedValue
		private long id;
	}

	public static class ManualIndexingOnly implements EntityIndexingInterceptor {
		@Override
		public IndexingOverride onAdd(Object entity) {
			return IndexingOverride.SKIP;
		}

		@Override
		public IndexingOverride onUpdate(Object entity) {
			return IndexingOverride.SKIP;
		}

		@Override
		public IndexingOverride onDelete(Object entity) {
			return IndexingOverride.APPLY_DEFAULT;
		}

		@Override
		public IndexingOverride onCollectionUpdate(Object entity) {
			return onUpdate( entity );
		}
	}
}
