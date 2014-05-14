/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.jdbc.Work;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class MultiClassesQueryLoaderTest extends SearchTestBase {

	@Test
	public void testObjectNotFound() throws Exception {
		Session sess = openSession();
		Transaction tx = sess.beginTransaction();
		Author author = new Author();
		author.setName( "Moo Cow" );
		sess.persist( author );

		tx.commit();
		sess.clear();
		sess.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				Statement statement = connection.createStatement();
				statement.executeUpdate( "DELETE FROM Author" );
				statement.close();
			}
		} );
		FullTextSession s = Search.getFullTextSession( sess );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.keywordAnalyzer );
		Query query = parser.parse( "name:moo" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Author.class, Music.class );
		List result = hibQuery.list();
		assertEquals( "Should have returned no author", 0, result.size() );

		tx.commit();
		s.close();
	}

	@Test
	public void testObjectTypeFiltering() throws Exception {
		Session sess = openSession();
		Transaction tx = sess.beginTransaction();
		Author author = new Author();
		author.setName( "Moo Cow" );
		Music music = new Music();
		music.addAuthor( author );
		music.setTitle( "The moo moo mooing under the stars" );
		Book book = new Book();
		book.setBody( "This is the story of the Moo Cow, who sang the moo moo moo at night" );
		book.setId( 1 );
		sess.persist( book );
		sess.persist( author );
		sess.persist( music );
		tx.commit();
		sess.clear();

		FullTextSession s = Search.getFullTextSession( sess );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.keywordAnalyzer );
		Query query = parser.parse( "name:moo OR title:moo OR body:moo" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Music.class );
		List result = hibQuery.list();
		assertEquals( "Should match the music only", 1, result.size() );
		hibQuery = s.createFullTextQuery( query, Author.class, Music.class );
		result = hibQuery.list();
		assertEquals( "Should match the author and music only", 2, result.size() );
		hibQuery = s.createFullTextQuery( query, Author.class, Music.class, Book.class );
		result = hibQuery.list();
		assertEquals( "Should match the author, music and book", 3, result.size() );
		hibQuery = s.createFullTextQuery( query );
		result = hibQuery.list();
		assertEquals( "Should match all types", 3, result.size() );

		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Author.class,
				Music.class,
				Book.class
		};
	}

}
