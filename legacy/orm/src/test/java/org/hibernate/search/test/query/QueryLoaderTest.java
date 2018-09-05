/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.util.List;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class QueryLoaderTest extends SearchTestBase {

	@Test
	public void testWithEagerCollectionLoad() throws Exception {
		Session sess = openSession();
		Transaction tx = sess.beginTransaction();
		Music music = new Music();
		music.setTitle( "Moo Goes The Cow" );
		Author author = new Author();
		author.setName( "Moo Cow" );
		music.addAuthor( author );
		sess.persist( author );
		author = new Author();
		author.setName( "Another Moo Cow" );
		music.addAuthor( author );
		sess.persist( author );
		author = new Author();
		author.setName( "A Third Moo Cow" );
		music.addAuthor( author );
		sess.persist( author );
		author = new Author();
		author.setName( "Random Moo Cow" );
		music.addAuthor( author );
		sess.persist( author );
		sess.save( music );

		Music music2 = new Music();
		music2.setTitle( "The Cow Goes Moo" );
		author = new Author();
		author.setName( "Moo Cow The First" );
		music2.addAuthor( author );
		sess.persist( author );
		author = new Author();
		author.setName( "Moo Cow The Second" );
		music2.addAuthor( author );
		sess.persist( author );
		author = new Author();
		author.setName( "Moo Cow The Third" );
		music2.addAuthor( author );
		sess.persist( author );
		author = new Author();
		author.setName( "Moo Cow The Fourth" );
		music2.addAuthor( author );
		sess.persist( author );
		sess.save( music2 );
		tx.commit();
		sess.clear();

		FullTextSession s = Search.getFullTextSession( sess );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.keywordAnalyzer );
		Query query = parser.parse( "title:moo" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Music.class );
		List result = hibQuery.list();
		assertEquals( "Should have returned 2 Books", 2, result.size() );
		music = (Music) result.get( 0 );
		assertEquals( "Book 1 should have four authors", 4, music.getAuthors().size() );
		music2 = (Music) result.get( 1 );
		assertEquals( "Book 2 should have four authors", 4, music2.getAuthors().size() );

		//cleanup
		music.getAuthors().clear();
		music2.getAuthors().clear();

		for ( Object o : s.createCriteria( Object.class ).list() ) {
			s.delete( o );
		}

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Author.class,
				Music.class
		};
	}
}
