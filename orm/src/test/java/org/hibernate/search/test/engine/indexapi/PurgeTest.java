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
package org.hibernate.search.test.engine.indexapi;

import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Criteria;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the PURGE and PURGE_ALL functionality.
 *
 * @author John Griffin
 * @author Hardy Ferentschik
 */
public class PurgeTest extends SearchTestCaseJUnit4 {

	@Before
	public void setUp() throws Exception {
		super.setUp();
		indexTestData();
	}

	@Test
	public void testPurgeById() throws Exception {
		assertNumberOfEntitiesForTypes( 2, Clock.class );
		assertNumberOfEntitiesForTypes( 2, Book.class );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		Criteria criteria = fullTextSession.createCriteria( Clock.class );
		criteria.setMaxResults( 1 );
		@SuppressWarnings("unchecked")
		List<Clock> clocks = (List<Clock>) criteria.list();
		// purge a single clock instance from the index
		fullTextSession.purge( Clock.class, clocks.get( 0 ).getId() );

		tx.commit();
		fullTextSession.close();

		assertNumberOfEntitiesForTypes( 1, Clock.class ); // only a single clock instance got purged
		assertNumberOfEntitiesForTypes( 2, Book.class );
	}

	@Test
	public void testPurgeAll() throws Exception {
		assertNumberOfEntitiesForTypes( 2, Clock.class );
		assertNumberOfEntitiesForTypes( 2, Book.class );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		// purge all clocks
		fullTextSession.purgeAll( Clock.class );

		tx.commit();
		fullTextSession.close();

		assertNumberOfEntitiesForTypes( 0, Clock.class );
		assertNumberOfEntitiesForTypes( 2, Book.class );

		fullTextSession = Search.getFullTextSession( openSession() );
		tx = fullTextSession.beginTransaction();

		// now purge all books
		fullTextSession.purgeAll( Book.class );

		tx.commit();
		fullTextSession.close();

		assertNumberOfEntitiesForTypes( 0, Clock.class );
		assertNumberOfEntitiesForTypes( 0, Book.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Clock.class,
				Author.class,
		};
	}

	private void assertNumberOfEntitiesForTypes(int expectedCount, Class<?>... types) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		org.hibernate.Query query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), types );
		@SuppressWarnings("unchecked")
		List<Object> results = (List<Object>) query.list();
		assertEquals( "incorrect test record", expectedCount, results.size() );

		tx.commit();
	}

	private void indexTestData() {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		// create a couple of clocks
		Clock clock = new Clock( 1, "Seiko" );
		fullTextSession.save( clock );
		clock = new Clock( 2, "Festina" );
		fullTextSession.save( clock );

		// create a couple of books
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		fullTextSession.save( book );
		book = new Book( 2, "La gloire de mon père", "Les deboires de mon père en vélo" );
		fullTextSession.save( book );

		tx.commit();
		fullTextSession.close();
	}
}
