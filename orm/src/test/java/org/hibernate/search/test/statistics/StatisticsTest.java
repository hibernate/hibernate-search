/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.Version;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * @author Yoann Rodiere
 */
@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2421 Support statistics with Elasticsearch
@RunWith( Parameterized.class )
public class StatisticsTest extends SearchTestBase {

	@Parameters(name = "Directory provider {0}")
	public static Object[] data() {
		return new Object[] {
			"local-heap",
			"filesystem" // Mainly to test getIndexSize()
		};
	}

	private final String directoryProviderName;

	public StatisticsTest(String directoryProviderName) {
		this.directoryProviderName = directoryProviderName;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { A.class, B.class };
	}

	@Override
	public void configure(java.util.Map<String,Object> settings) {
		settings.put( Environment.GENERATE_STATS, Boolean.TRUE.toString() );
		settings.put( "hibernate.search.default.directory_provider", directoryProviderName );
	}

	@After
	public void cleanupStats() {
		getStatistics().setStatisticsEnabled( true );
		getSearchFactory().getStatistics().clear();
	}

	@Test
	public void enabled() {
		assertTrue( getStatistics().isStatisticsEnabled() );

		getStatistics().setStatisticsEnabled( false );
		assertFalse( getStatistics().isStatisticsEnabled() );

		getStatistics().setStatisticsEnabled( true );
		assertTrue( getStatistics().isStatisticsEnabled() );
	}

	@Test
	public void searchVersion() {
		assertEquals( Version.getVersionString(), getStatistics().getSearchVersion() );
	}

	@Test
	public void indexedClassNames() {
		Set<String> indexedClassNames = getStatistics().getIndexedClassNames();
		assertEquals( 2, indexedClassNames.size() );
		assertTrue( indexedClassNames.contains( A.class.getName() ) );
		assertTrue( indexedClassNames.contains( B.class.getName() ) );
	}

	@Test
	public void indexedEntitiesCount() {
		Map<String, Integer> indexedEntitiesCount = getStatistics().indexedEntitiesCount();
		assertEquals( 2, indexedEntitiesCount.size() );
		assertEquals( (Integer) 0, indexedEntitiesCount.get( A.class.getName() ) );
		assertEquals( (Integer) 0, indexedEntitiesCount.get( B.class.getName() ) );

		assertEquals( 0, getStatistics().getNumberOfIndexedEntities( A.class.getName() ) );
		assertEquals( 0, getStatistics().getNumberOfIndexedEntities( B.class.getName() ) );

		Session s = openSession();
		try {
			A entity = new A();
			entity.id = 1L;
			Transaction tx = s.beginTransaction();
			s.persist( entity );
			tx.commit();
		}
		finally {
			s.close();
		}

		indexedEntitiesCount = getStatistics().indexedEntitiesCount();
		assertEquals( 2, indexedEntitiesCount.size() );
		assertEquals( (Integer) 1, indexedEntitiesCount.get( A.class.getName() ) );
		assertEquals( (Integer) 0, indexedEntitiesCount.get( B.class.getName() ) );

		assertEquals( 1, getStatistics().getNumberOfIndexedEntities( A.class.getName() ) );
	}

	@Test
	public void queryExecution() {
		Session s = openSession();
		try {
			FullTextSession session = Search.getFullTextSession( s );
			FullTextQuery query = session.createFullTextQuery( matchAll(), A.class );

			assertEquals( 0, getStatistics().getSearchQueryExecutionCount() );
			assertEquals( 0, getStatistics().getSearchQueryExecutionAvgTime() );
			assertEquals( 0, getStatistics().getSearchQueryExecutionMaxTime() );
			assertNull( getStatistics().getSearchQueryExecutionMaxTimeQueryString() );

			query.getResultList();

			assertEquals( 1, getStatistics().getSearchQueryExecutionCount() );
			assertNotEquals( 0, getStatistics().getSearchQueryExecutionAvgTime() );
			assertNotEquals( 0, getStatistics().getSearchQueryExecutionMaxTime() );
			assertNotNull( getStatistics().getSearchQueryExecutionMaxTimeQueryString() );

			query = session.createFullTextQuery( matchAll(), A.class );
			query.getResultList();

			assertEquals( 2, getStatistics().getSearchQueryExecutionCount() );
			assertNotEquals( 0, getStatistics().getSearchQueryExecutionAvgTime() );
			assertNotEquals( 0, getStatistics().getSearchQueryExecutionMaxTime() );
			assertNotNull( getStatistics().getSearchQueryExecutionMaxTimeQueryString() );
		}
		finally {
			s.close();
		}
	}

	@Test
	public void objectLoading() {
		Session s = openSession();
		try {
			Transaction tx = s.beginTransaction();
			A entity = new A();
			entity.id = 1L;
			s.persist( entity );
			tx.commit();

			FullTextSession session = Search.getFullTextSession( s );
			FullTextQuery query = session.createFullTextQuery( matchAll(), A.class );

			assertEquals( 0, getStatistics().getObjectsLoadedCount() );
			assertEquals( 0, getStatistics().getObjectLoadingExecutionAvgTime() );
			assertEquals( 0, getStatistics().getObjectLoadingExecutionMaxTime() );
			assertEquals( 0, getStatistics().getObjectLoadingTotalTime() );

			query.getResultList();

			assertEquals( 1, getStatistics().getObjectsLoadedCount() );
			assertNotEquals( 0, getStatistics().getObjectLoadingExecutionAvgTime() );
			assertNotEquals( 0, getStatistics().getObjectLoadingExecutionMaxTime() );
			assertNotEquals( 0, getStatistics().getObjectLoadingTotalTime() );

			query = session.createFullTextQuery( matchAll(), A.class );
			query.getResultList();

			assertEquals( 2, getStatistics().getObjectsLoadedCount() );
			assertNotEquals( 0, getStatistics().getObjectLoadingExecutionAvgTime() );
			assertNotEquals( 0, getStatistics().getObjectLoadingExecutionMaxTime() );
			assertNotEquals( 0, getStatistics().getObjectLoadingTotalTime() );
		}
		finally {
			s.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2630")
	public void objectLoading_multiClassesQueryLoader_singleResult() {
		Session s = openSession();
		try {
			Transaction tx = s.beginTransaction();
			A entity = new A();
			entity.id = 1L;
			s.persist( entity );
			tx.commit();

			FullTextSession session = Search.getFullTextSession( s );
			FullTextQuery query = session.createFullTextQuery( matchAll(), A.class, B.class );

			assertEquals( 0, getStatistics().getObjectsLoadedCount() );

			query.getResultList();

			assertEquals( 1, getStatistics().getObjectsLoadedCount() );

			query = session.createFullTextQuery( matchAll(), A.class, B.class );
			query.getResultList();

			assertEquals( 2, getStatistics().getObjectsLoadedCount() );
		}
		finally {
			s.close();
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	@TestForIssue(jiraKey = "HSEARCH-2631")
	public void objectLoading_singleClassQueryLoader_criteria_iterate() {
		Session s = openSession();
		try {
			Transaction tx = s.beginTransaction();
			A entity = new A();
			entity.id = 1L;
			s.persist( entity );
			tx.commit();

			FullTextSession session = Search.getFullTextSession( s );
			FullTextQuery query = session.createFullTextQuery( matchAll() )
					.setCriteriaQuery( session.createCriteria( A.class ) );

			assertEquals( 0, getStatistics().getObjectsLoadedCount() );

			Iterator<?> iterator = query.iterate();
			iterator.next();

			assertEquals( 1, getStatistics().getObjectsLoadedCount() );

			query = session.createFullTextQuery( matchAll(), A.class );
			iterator = query.iterate();
			iterator.next();

			assertEquals( 2, getStatistics().getObjectsLoadedCount() );
		}
		finally {
			s.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2014")
	public void indexSize() {
		long currentSizeForA = getStatistics().getIndexSize( A.INDEX_NAME );
		long currentSizeForB = getStatistics().getIndexSize( B.INDEX_NAME );
		// Don't assume anything about the size of an empty index; it may not be 0

		Map<String, Long> indexSizes = getStatistics().indexSizes();
		assertEquals( 2, indexSizes.size() );
		assertEquals( (Long) currentSizeForA, indexSizes.get( A.INDEX_NAME ) );
		assertEquals( (Long) currentSizeForB, indexSizes.get( B.INDEX_NAME ) );

		try (Session s = openSession()) {
			A entity = new A();
			entity.id = 1L;
			Transaction tx = s.beginTransaction();
			s.persist( entity );
			tx.commit();
		}

		long previousSizeForA = currentSizeForA;
		long previousSizeForB = currentSizeForB;
		currentSizeForA = getStatistics().getIndexSize( A.INDEX_NAME );
		currentSizeForB = getStatistics().getIndexSize( B.INDEX_NAME );
		assertTrue( currentSizeForA > previousSizeForA );
		assertEquals( previousSizeForB, currentSizeForB );

		indexSizes = getStatistics().indexSizes();
		assertEquals( 2, indexSizes.size() );
		assertEquals( (Long) currentSizeForA, indexSizes.get( A.INDEX_NAME ) );
		assertEquals( (Long) currentSizeForB, indexSizes.get( B.INDEX_NAME ) );

		try (Session s = openSession()) {
			A entity = new A();
			entity.id = 2L;
			Transaction tx = s.beginTransaction();
			s.persist( entity );
			tx.commit();
		}

		previousSizeForA = currentSizeForA;
		previousSizeForB = currentSizeForB;
		currentSizeForA = getStatistics().getIndexSize( A.INDEX_NAME );
		currentSizeForB = getStatistics().getIndexSize( B.INDEX_NAME );
		assertTrue( currentSizeForA > previousSizeForA );
		assertEquals( previousSizeForB, currentSizeForB );

		indexSizes = getStatistics().indexSizes();
		assertEquals( 2, indexSizes.size() );
		assertEquals( (Long) currentSizeForA, indexSizes.get( A.INDEX_NAME ) );
		assertEquals( (Long) currentSizeForB, indexSizes.get( B.INDEX_NAME ) );
	}

	private Statistics getStatistics() {
		return getSearchFactory().getStatistics();
	}

	private Query matchAll() {
		return new MatchAllDocsQuery();
	}

	@Entity
	@Indexed
	private static class A {
		public static final String INDEX_NAME = A.class.getName();

		@Id
		private Long id;

		@Field
		private String field;
	}

	@Entity
	@Indexed(index = B.INDEX_NAME)
	private static class B {
		public static final String INDEX_NAME = "B";

		@Id
		private Long id;

		@Field
		private String field;
	}

}
