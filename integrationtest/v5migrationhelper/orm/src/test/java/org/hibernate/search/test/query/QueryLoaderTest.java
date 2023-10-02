/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class QueryLoaderTest extends SearchTestBase {

	@Test
	void testWithEagerCollectionLoad() throws Exception {
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
		assertThat( result ).as( "Should have returned 2 Books" ).hasSize( 2 );
		music = (Music) result.get( 0 );
		assertThat( music.getAuthors() ).as( "Book 1 should have four authors" ).hasSize( 4 );
		music2 = (Music) result.get( 1 );
		assertThat( music2.getAuthors() ).as( "Book 2 should have four authors" ).hasSize( 4 );

		//cleanup
		music.getAuthors().clear();
		music2.getAuthors().clear();

		for ( Object o : listAll( s, Object.class ) ) {
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
