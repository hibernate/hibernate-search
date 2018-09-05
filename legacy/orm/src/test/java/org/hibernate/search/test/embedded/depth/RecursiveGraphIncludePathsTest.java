/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.depth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.backend.LeakingLocalBackend;
import org.junit.Test;

/**
 * Verify the engine respects the includePaths parameter {@link IndexedEmbedded#includePaths()} without indexing larger
 * graphs than requested.
 */
public class RecursiveGraphIncludePathsTest extends SearchTestBase {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2074")
	public void testCorrectDepthIndexedWithIncludePath() {
		prepareSocialGraph();

		verifyMatchExistsWithName( "name", "Ross", 0L );
		verifyMatchExistsWithName( "name", "Rachel", 5L );
		verifyMatchExistsWithName( "name", "Gunter", 6L );

		verifyMatchExistsWithName( "friends.name", "Ross", 1L, 2L, 3L, 4L, 5L );
		verifyMatchExistsWithName( "friends.name", "Rachel", 0L, 1L, 2L, 3L, 4L, 6L );
		verifyNoMatchExists( "friends.name", "Gunter" );

		LeakingLocalBackend.reset();
		renamePerson( 5L, "Rachelita" ); // Rename Rachel, friend to anyone
		assertEquals( 1, countWorksDoneOnPerson( 0L ) );
		assertEquals( 1, countWorksDoneOnPerson( 1L ) );
		assertEquals( 1, countWorksDoneOnPerson( 2L ) );
		assertEquals( 1, countWorksDoneOnPerson( 3L ) );
		assertEquals( 1, countWorksDoneOnPerson( 4L ) );
		assertEquals( 1, countWorksDoneOnPerson( 5L ) );
		assertEquals( 1, countWorksDoneOnPerson( 6L ) );

		LeakingLocalBackend.reset();
		renamePerson( 0L, "Rossito" ); // Rename Ross, friend to anyone but Gunter
		assertEquals( 1, countWorksDoneOnPerson( 0L ) );
		assertEquals( 1, countWorksDoneOnPerson( 1L ) );
		assertEquals( 1, countWorksDoneOnPerson( 2L ) );
		assertEquals( 1, countWorksDoneOnPerson( 3L ) );
		assertEquals( 1, countWorksDoneOnPerson( 4L ) );
		assertEquals( 1, countWorksDoneOnPerson( 5L ) );

		// 6L (Gunter) does not embed any field from 0L (Ross/Rossito)
		// It should not be reindexed
		assertEquals( 0, countWorksDoneOnPerson( 6L ) );
	}

	/**
	 * rename a person having id to a new name
	 */
	private void renamePerson(Long id, String newName) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			SocialPerson person = fullTextSession.load( SocialPerson.class, id );
			person.setName( newName );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	/**
	 * asserts no results are returned for fieldName having fieldValue
	 */
	void verifyNoMatchExists(String fieldName, String fieldValue) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new TermQuery( new Term( fieldName, fieldValue ) );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			int resultSize = fullTextQuery.getResultSize();
			assertEquals( 0, resultSize );
			@SuppressWarnings("unchecked")
			List<SocialPerson> list = fullTextQuery.list();
			assertEquals( 0, list.size() );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	void verifyMatchExistsWithName(String fieldName, String fieldValue, Long... expectedIds) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new TermQuery( new Term( fieldName, fieldValue ) );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			int resultSize = fullTextQuery.getResultSize();
			assertEquals( expectedIds.length, resultSize );
			@SuppressWarnings("unchecked")
			List<SocialPerson> list = fullTextQuery.list();

			assertEquals( expectedIds.length, list.size() );

			List<Long> expectIdsList = Arrays.asList( expectedIds );
			for ( SocialPerson person : list ) {
				assertTrue( expectIdsList.contains( person.getId() ) );
			}

			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	private void prepareSocialGraph() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		SocialPerson[] ps = new SocialPerson[7];
		ps[0] = new SocialPerson( 0L, "Ross" );
		ps[1] = new SocialPerson( 1L, "Chandler" );
		ps[2] = new SocialPerson( 2L, "Joey" );
		ps[3] = new SocialPerson( 3L, "Phoebe" );
		ps[4] = new SocialPerson( 4L, "Monica" );
		ps[5] = new SocialPerson( 5L, "Rachel" );

		ps[6] = new SocialPerson( 6L, "Gunter" );

		// Friends
		for ( int i = 0; i < 6; i++ ) {
			for ( int j = 0; j < 6; j++ ) {
				if ( i != j ) {
					ps[i].addFriends( ps[j] );
				}
			}
		}

		// Lonely person
		ps[6].addFriends( ps[5] );

		for ( int i = 0; i < ps.length; i++ ) {
			session.save( ps[i] );
		}

		transaction.commit();
		session.close();
		for ( int i = 1; i < ps.length; i++ ) {
			assertEquals( 1, countWorksDoneOnPerson( Long.valueOf( i ) ) );
		}
	}

	private int countWorksDoneOnPerson(Long pk) {
		List<LuceneWork> processedQueue = LeakingLocalBackend.getLastProcessedQueue();
		int count = 0;
		for ( LuceneWork luceneWork : processedQueue ) {
			Serializable id = luceneWork.getId();
			if ( pk.equals( id ) ) {
				count++;
			}
		}
		return count;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { SocialPerson.class };
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( "hibernate.search.default.worker.backend", LeakingLocalBackend.class.getName() );
	}

}
