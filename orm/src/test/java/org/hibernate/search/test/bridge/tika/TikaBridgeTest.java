/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.bridge.tika;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TikaBridge;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TikaMetadataProcessor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Assert;

/**
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class TikaBridgeTest extends SearchTestCase {
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

	public void testIndexMp3MetaTags() throws Exception {
		Session session = openSession();

		persistSong( session );
		searchSong( session );

		session.close();
	}

	public void testIndexMp3MetaTagsDSL() throws Exception {
		Session session = openSession();

		persistSong( session );
		searchSongDsl( session );

		session.close();
	}

	public void testIndexMp3MetaTagsDSLErrorMessage() throws Exception {
		Session session = openSession();

		persistSong( session );
		searchSongDsl( session );

		session.close();
	}

	public void testUnsupportedTypeForTikaBridge() throws Exception {
		Session session = openSession();

		try {
			Transaction tx = session.beginTransaction();
			session.save( new Foo() );
			tx.commit();
			fail();
		}
		catch (HibernateException e) {
			// hmm, a lot of exception wrapping going on
			assertTrue( e.getCause() instanceof BridgeException );
			BridgeException bridgeException = (BridgeException) e.getCause();
			assertTrue( e.getCause() instanceof SearchException );
			SearchException searchException = (SearchException) bridgeException.getCause();
			assertTrue( "Wrong root cause", searchException.getMessage().startsWith( "HSEARCH000151" ) );
		}
		finally {
			session.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class, Song.class
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
		Date now = new Date();

		public long getId() {
			return id;
		}

		public Date getNow() {
			return now;
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
