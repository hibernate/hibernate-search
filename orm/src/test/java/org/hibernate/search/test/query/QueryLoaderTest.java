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

import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Emmanuel Bernard
 */
public class QueryLoaderTest extends SearchTestCase {

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
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.keywordAnalyzer );
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Author.class,
				Music.class
		};
	}
}
