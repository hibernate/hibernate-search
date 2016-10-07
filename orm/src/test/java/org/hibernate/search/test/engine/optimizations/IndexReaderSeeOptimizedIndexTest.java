/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.optimizations;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test that the IndexReader can see an optmized index before the factory is closed.
 *
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "HSEARCH-1681")
@Category(SkipOnElasticsearch.class) // IndexReaders are specific to the Lucene backend
public class IndexReaderSeeOptimizedIndexTest extends SearchTestBase {

	private String indexBasePath;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			String band = "Impaled Northern Moonforest";

			SongWithLongTitle song1 = new SongWithLongTitle();
			song1.setId( 1L );
			song1.setBand( band );
			song1.setTitle( "Gazing At The Blasphemous Moon While Perched Atop A Very Very Very Very Very Very Very Forsaken Crest Of The Northern Mountain" );

			SongWithLongTitle song2 = new SongWithLongTitle();
			song2.setId( 2L );
			song2.setBand( band );
			song2.setTitle( "Summoning The Unholy Frozen Winterdemons To The Grimmest And Most Frostbitten Inverted Forest Of Abazagorath" );

			SongWithLongTitle song3 = new SongWithLongTitle();
			song3.setId( 3L );
			song3.setBand( band );
			song3.setTitle( "Awaiting The Frozen Blasphemy Of The Necroyeti's Lusting Necrobation Upon The Altar Of Voxrfszzzisnzf" );

			session.persist( song1 );
			session.persist( song2 );
			session.persist( song3 );
			tx.commit();
		}
	}

	@After
	public void deleteAll() throws Exception {
		openSessionFactory();
		deleteEntities( 1L, 2L, 3L );
		super.tearDown();
	}

	private void deleteEntities(Serializable... ids) {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			for ( Serializable id : ids ) {
				SongWithLongTitle entity = session.get( SongWithLongTitle.class, id );
				if ( entity != null ) {
					session.delete( entity );
				}
			}
			tx.commit();
		}
	}

	public static long folderSize(File directory) {
		long length = 0;
		for ( File file : directory.listFiles() ) {
			if ( file.isFile() ) {
				length += file.length();
			}
			else {
				length += folderSize( file );
			}
		}
		return length;
	}

	@Override
	public void configure(Map<String, Object> settings) {
		settings.put( "hibernate.search.default.directory_provider", "filesystem" );
		indexBasePath = (String) settings.get( "hibernate.search.default.indexBase" );
	}

	@Test
	public void testFolderSize() throws Exception {
		File indexFolder = new File( indexBasePath );
		long initialFolderSize = folderSize( indexFolder );

		try ( Session session = openSession() ) {
			session.beginTransaction();
			SearchFactory searchFactory = Search.getFullTextSession( session ).getSearchFactory();
			searchFactory.optimize();
			session.getTransaction().commit();
		}

		long afterSessionClosed = folderSize( indexFolder );
		closeSessionFactory();
		long afterFactoryClosed = folderSize( indexFolder );

		assertThat( afterSessionClosed ).isEqualTo( afterFactoryClosed );
		assertThat( afterFactoryClosed ).isLessThan( initialFolderSize );
	}

	@Test
	public void testIndexReaderAccessOptimizedIndex() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			session.delete( session.load( SongWithLongTitle.class, 1L ) );
			session.getTransaction().commit();
			session.clear();

			session.beginTransaction();
			SearchFactory searchFactory = Search.getFullTextSession( session ).getSearchFactory();
			try (IndexReader indexReader = indexReader( searchFactory )) {
				assertThat( indexReader.hasDeletions() )
					.as( "IndexReader should see the deletions before the optimization" )
					.isTrue();

				searchFactory.optimize();
				session.getTransaction().commit();
			}

			try (IndexReader indexReader = indexReader( searchFactory )) {
				assertThat( indexReader.hasDeletions() )
					.as( "IndexReader should see some deletions after optimization" )
					.isFalse();
			}

		}
		finally {
			closeSessionFactory();
		}
	}

	private IndexReader indexReader(SearchFactory searchFactory) {
		return searchFactory.getIndexReaderAccessor().open( SongWithLongTitle.class );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SongWithLongTitle.class };
	}

}
