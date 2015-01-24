/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge.tika;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TikaBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TikaMetadataProcessor;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class TikaBridgeTest extends SearchTestBase {
	private static final String TEST_MP3_DOCUMENT = "/org/hibernate/search/test/bridge/tika/mysong.mp3";
	private static final String PATH_TO_TEST_MP3;

	static {
		try {
			File mp3File = new File( TikaBridgeTest.class.getResource( TEST_MP3_DOCUMENT ).toURI() );
			PATH_TO_TEST_MP3 = mp3File.getAbsolutePath();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( "Unable to determine file path for test document" );
		}
	}

	@Test
	public void testIndexMp3MetaTags() throws Exception {
		Session session = openSession();

		persistSong( session );
		searchSong( session );

		session.close();
	}

	@Test
	public void testIndexMp3MetaTagsDSL() throws Exception {
		Session session = openSession();

		persistSong( session );
		searchSongDsl( session );

		session.close();
	}

	@Test
	public void testIndexMp3MetaTagsDSLErrorMessage() throws Exception {
		Session session = openSession();

		persistSong( session );
		searchSongDsl( session );

		session.close();
	}

	@Test
	public void testUnsupportedTypeForTikaBridge() throws Exception {
		SearchConfiguration conf = new HibernateManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", "ram" )
				.addProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().toString() )
				.addClass( Foo.class );
		boolean throwException = false;
		try {
			SearchIntegrator sf = new SearchIntegratorBuilder().configuration( conf ).buildSearchIntegrator();
			sf.close();
		}
		catch (SearchException e) {
			assertThat( e.getMessage() ).startsWith( "HSEARCH000151" );
			throwException = true;
		}
		assertThat( throwException ).isTrue();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Song.class
		};
	}

	private void persistSong(Session session) {
		Transaction tx = session.beginTransaction();
		Song mySong = new Song( PATH_TO_TEST_MP3 );
		session.save( mySong );
		tx.commit();
	}

	@SuppressWarnings("unchecked")
	private void searchSong(Session session) throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction tx = session.beginTransaction();
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				XMPDM.ARTIST.getName(),
				TestConstants.standardAnalyzer
		);
		Query query = parser.parse( "Emmanuel" );


		List<Song> result = fullTextSession.createFullTextQuery( query ).list();
		assertEquals( "Emmanuel is not an artist", 0, result.size() );

		query = parser.parse( "Hardy" );

		result = fullTextSession.createFullTextQuery( query ).list();
		assertEquals( "Hardy is the artist", 1, result.size() );

		tx.commit();
	}

	private void searchSongDsl(Session session) throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction tx = session.beginTransaction();

		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Song.class ).get();

		Query queryEmmanuel = queryBuilder.keyword().onField( "mp3FileName" ).ignoreFieldBridge().matching( "Emmanuel" ).createQuery();

		List<Song> result = fullTextSession.createFullTextQuery( queryEmmanuel ).list();
		assertEquals( "Emmanuel is not an artist", 0, result.size() );

		Query queryHardy = queryBuilder.keyword().onField( "mp3FileName" ).ignoreFieldBridge().matching( "Hardy" ).createQuery();

		result = fullTextSession.createFullTextQuery( queryHardy ).list();
		assertEquals( "Hardy is the artist", 1, result.size() );

		tx.commit();
	}

	@TestForIssue(jiraKey = "HSEARCH-1256")
	private void searchSongDslErrorMessage(Session session) throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction tx = session.beginTransaction();

		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Song.class ).get();

		boolean exceptionCaught = false;
		try {
			queryBuilder.keyword().onField( "mp3FileName" ).ignoreFieldBridge().matching( "Emmanuel" ).createQuery();
		}
		catch (Exception e) {
			exceptionCaught = true;
			Assert.assertTrue( e instanceof SearchException );
			Assert.assertTrue( e.getMessage().contains( "The FieldBridge must be a TwoWayFieldBridge or you have to enable the ignoreFieldBridge option when defining a Query" ) );
		}
		Assert.assertTrue( exceptionCaught );
		tx.commit();
	}

	@Entity
	@Indexed
	public static class Foo {
		@Id
		@GeneratedValue
		long id;

		@Field
		@TikaBridge
		Bar someBar = new Bar();

		public long getId() {
			return id;
		}

		public Bar getSomeBar() {
			return someBar;
		}

		public static class Bar implements Serializable {
		}
	}

	@Entity
	@Indexed
	public static class Song {
		@Id
		@GeneratedValue
		long id;

		@Field
		@TikaBridge(metadataProcessor = Mp3TikaMetadataProcessor.class)
		String mp3FileName;

		public Song(String mp3FileName) {
			this.mp3FileName = mp3FileName;
		}
		public Song() {
		}

		public String getMp3FileName() {
			return mp3FileName;
		}
	}

	public static class Mp3TikaMetadataProcessor implements TikaMetadataProcessor {
		@Override
		public Metadata prepareMetadata() {
			return new Metadata();
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions, Metadata metadata) {
			luceneOptions.addFieldToDocument( XMPDM.ARTIST.getName(), metadata.get( XMPDM.ARTIST ), document );
		}
	}
}
