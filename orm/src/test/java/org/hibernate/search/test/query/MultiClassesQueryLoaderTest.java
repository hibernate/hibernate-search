/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.jdbc.Work;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class MultiClassesQueryLoaderTest extends SearchTestCase {

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
