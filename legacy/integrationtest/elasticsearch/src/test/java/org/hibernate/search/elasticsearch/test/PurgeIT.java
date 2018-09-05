/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.apache.log4j.Level;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class) // Do not use the CustomRunner, it messes with rules (by reusing the same test instance)
public class PurgeIT extends SearchTestBase {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private FullTextSession fullTextSession;

	@Override
	@Before
	public void setUp() throws Exception {
		// Make sure that no automatic refresh will occur during the test
		elasticsearchClient.template( "no_automatic_refresh" )
				.create(
						"*",
						JsonBuilder.object()
								.add( "index", JsonBuilder.object()
										.addProperty( "refresh_interval", "-1" )
								)
								.build()
				);

		super.setUp();

		createPersistAndIndexTestData();
	}

	/*
	 * Test executing multiple purges without explicitly refreshing in-between.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2761")
	public void multiplePurges() throws Exception {
		flush();
		List<Level1> all = getAll();
		assertEquals( "Wrong total number of entries", 3, all.size() );

		// Expect 0 failure in the backend threads
		logged.expectLevelMissing( Level.ERROR );

		Transaction tx = fullTextSession.beginTransaction();

		// Order is significant to reproduce the issue, see HSEARCH-2761
		fullTextSession.purgeAll( Level2.class );
		fullTextSession.purgeAll( Level3.class );
		fullTextSession.purgeAll( Level1.class );

		tx.commit();

		flush();
		all = getAll();
		assertEquals( "Wrong total number of entries. Index should be empty after purge.", 0, all.size() );

		tx = fullTextSession.beginTransaction();
		fullTextSession.createIndexer()
				.batchSizeToLoadObjects( 25 )
				.threadsToLoadObjects( 1 )
				.optimizeOnFinish( true )
				.startAndWait();
		tx.commit();

		flush();
		all = getAll();
		assertEquals( "Wrong total number of entries.", 3, all.size() );
	}

	/*
	 * Test executing a purge after a write without explicitly refreshing in-between.
	 */
	@Test
	public void writeThenPurge() throws Exception {
		flush();
		List<Level1> all = getAll();
		assertEquals( "Wrong total number of entries", 3, all.size() );

		// Expect 0 failure in the backend threads
		logged.expectLevelMissing( Level.ERROR );

		Transaction tx = fullTextSession.beginTransaction();

		fullTextSession.index( fullTextSession.get( Level1.class, 1L ) );

		tx.commit();

		tx = fullTextSession.beginTransaction();

		fullTextSession.purgeAll( Level1.class );

		tx.commit();

		flush();
		all = getAll();
		assertEquals( "Wrong total number of entries. Index should be empty after purge.", 0, all.size() );

		tx = fullTextSession.beginTransaction();
		fullTextSession.createIndexer()
				.batchSizeToLoadObjects( 25 )
				.threadsToLoadObjects( 1 )
				.optimizeOnFinish( true )
				.startAndWait();
		tx.commit();

		flush();
		all = getAll();
		assertEquals( "Wrong total number of entries.", 3, all.size() );
	}

	/**
	 * Perform a flush, which implies a refresh
	 */
	private void flush() {
		IndexManager indexManager = getExtendedSearchIntegrator().getIndexBindings().get( Level1.class )
				.getIndexManagerSelector().all().iterator().next();
		indexManager.performOperations( Collections.singletonList( FlushLuceneWork.INSTANCE ), null );
	}

	private void createPersistAndIndexTestData() {
		Level1 level1 = new Level1();
		level1.id = 1L;
		level1.text = "Level 1";

		Level2 level2 = new Level2();
		level2.id = 2L;
		level2.text = "Level 2" ;

		Level3 level3 = new Level3();
		level3.id = 3L;
		level3.text = "Level 3";

		fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.persist( level1 );
		fullTextSession.persist( level2 );
		fullTextSession.persist( level3 );
		tx.commit();

		fullTextSession.clear();
	}

	@SuppressWarnings("unchecked")
	private List<Level1> getAll() {
		Query query = new MatchAllDocsQuery();
		return fullTextSession.createFullTextQuery( query, Level1.class ).list();
	}

	@Override
	public void configure(Map<String, Object> settings) {
		// This test should work fine even without refreshes after writes
		settings.put( "hibernate.search.default." + ElasticsearchEnvironment.REFRESH_AFTER_WRITE, "false" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Level1.class,
				Level2.class,
				Level3.class
		};
	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	@Indexed
	private static class Level1 {
		@Id
		protected Long id;

		@Field
		protected String text;
	}

	@Entity
	@Indexed
	private static class Level2 extends Level1 {

	}

	@Entity
	@Indexed
	private static class Level3 extends Level2 {

	}
}
