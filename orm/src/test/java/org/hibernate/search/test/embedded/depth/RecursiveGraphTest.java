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
package org.hibernate.search.test.embedded.depth;

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.LeakingLuceneBackend;

/**
 * Verify the engine respects the depth parameter {@link IndexedEmbedded#depth()} without indexing larger graphs than
 * requested.
 * See the Genealogy graph at http://en.wikipedia.org/wiki/John,_King_of_England to visualize the data
 * used as test case.
 *
 * @author Sanne Grinovero
 */
public class RecursiveGraphTest extends SearchTestCase {

	public void testCorrectDepthIndexed() {
		prepareGenealogyTree();
		verifyMatchExistsWithName( 1L, "name", "John of England" );
		verifyNoMatchExists( "parents.name", "John of England" );
		verifyMatchExistsWithName( 1L, "parents.name", "Henry II of England" );
		verifyMatchExistsWithName( 1L, "parents.parents.name", "Geoffrey V of Anjou" );
		verifyMatchExistsWithName( 2L, "parents.parents.name", "Fulk V of Anjou" );
		verifyNoMatchExists( "parents.parents.parents.name", "Fulk V of Anjou" );

		LeakingLuceneBackend.reset();
		renamePerson( 1L, "John Lackland" );
		assertEquals( 1, countWorksDoneOnPerson( 1L ) );
		assertEquals( 0, countWorksDoneOnPerson( 2L ) );

		LeakingLuceneBackend.reset();
		renamePerson( 2L, "Henry II of New England" );
		assertEquals( 1, countWorksDoneOnPerson( 1L ) );
		assertEquals( 1, countWorksDoneOnPerson( 2L ) );

		LeakingLuceneBackend.reset();
		renamePerson( 16L, "Fulk 4th of Anjou" );
		assertEquals( 1, countWorksDoneOnPerson( 16L ) );
		assertEquals( 0, countWorksDoneOnPerson( 17L ) );
		assertEquals( 1, countWorksDoneOnPerson( 8L ) );
		assertEquals( 1, countWorksDoneOnPerson( 4L ) );
		assertEquals( 0, countWorksDoneOnPerson( 2L ) );
		assertEquals( 0, countWorksDoneOnPerson( 1L ) );
	}

	/**
	 * rename a person having id to a new name
	 */
	private void renamePerson(Long id, String newName) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Person kingJohn = (Person) fullTextSession.load( Person.class, id );
			kingJohn.setName( newName );
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
			List<Person> list = fullTextQuery.list();
			assertEquals( 0, list.size() );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	void verifyMatchExistsWithName(Long expectedId, String fieldName, String fieldValue) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new TermQuery( new Term( fieldName, fieldValue ) );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			int resultSize = fullTextQuery.getResultSize();
			assertEquals( 1, resultSize );
			List<Person> list = fullTextQuery.list();
			assertEquals( 1, list.size() );
			assertEquals( expectedId, list.get( 0 ).getId() );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	void prepareGenealogyTree() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Person[] ps = new Person[18];
		// array index starting from 1 to match ids of picture at http://en.wikipedia.org/wiki/John,_King_of_England
		ps[1] = new Person( 1L, "John of England" );
		ps[2] = new Person( 2L, "Henry II of England" );
		ps[3] = new Person( 3L, "Eleanor of Aquitaine" );
		ps[4] = new Person( 4L, "Geoffrey V of Anjou" );
		ps[5] = new Person( 5L, "Empress Matilda" );
		ps[6] = new Person( 6L, "William X of Aquitaine" );
		ps[7] = new Person( 7L, "Aenor de Châtellerault" );
		ps[8] = new Person( 8L, "Fulk V of Anjou" );
		ps[9] = new Person( 9L, "Ermengarde of Maine" );
		ps[10] = new Person( 10L, "Henry I of England" );
		ps[11] = new Person( 11L, "Matilda of Scotland" );
		ps[12] = new Person( 12L, "William IX of Aquitaine" );
		ps[13] = new Person( 13L, "Philippa of Toulouse" );
		ps[14] = new Person( 14L, "Aimery I of Châttellerault" );
		ps[15] = new Person( 15L, "Dangereuse de L'Isle Bouchard" );
		ps[16] = new Person( 16L, "Fulk IV of Anjou" );
		ps[17] = new Person( 17L, "Bertrade de Montfort" );

		ps[1].addParents( ps[2], ps[3] );
		ps[2].addParents( ps[4], ps[5] );
		ps[3].addParents( ps[6], ps[7] );
		ps[4].addParents( ps[8], ps[9] );
		ps[5].addParents( ps[10], ps[11] );
		ps[6].addParents( ps[12], ps[13] );
		ps[7].addParents( ps[14], ps[15] );
		ps[8].addParents( ps[16], ps[17] );
		for ( int i = 1; i < 18; i++ ) {
			session.save( ps[i] );
		}
		transaction.commit();
		session.close();
		for ( int i = 1; i < 18; i++ ) {
			assertEquals( 1, countWorksDoneOnPerson( Long.valueOf( i ) ) );
		}
	}

	private int countWorksDoneOnPerson(Long pk) {
		List<LuceneWork> processedQueue = LeakingLuceneBackend.getLastProcessedQueue();
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() );
	}

}
