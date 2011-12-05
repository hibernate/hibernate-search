/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.search.test.embedded.depth;

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.LeakingLuceneBackend;

/**
 * <pre>
 * FAMILY: {@link IndexEmbedded#depth} = 2
 *
 * 13 Philippa of Toulouse __________
 * 12 William IX of Aquitaine _______|- 6 William X of Aquitaine __
 *                                                                 |
 * 14 Aimery I of Châttellerault ____                              |- 3 Eleanor of Aquitaine _________________________
 * 15 Dangereuse de L'Isle Bouchard _|- 7 Aenor de Châtellerault __|                                                  |
 *                                                                                                                    |
 *                                                                                                                    |
 *                                                                                                                    |-1 John of England
 * 16 Fulk IV of Anjou ______________                                                                                 |
 * 17 Bertrade de Montfort __________|- 8 Fulk V of Anjou ________                                                    |
 *                                      9 Ermengarde of Maine ____|- 4 Geoffrey V of Anjou _                          |
 *                                                                   5 Empress Matilda _____|- 2 Henry II of England _|
 * </pre>
 *
 * <pre>
 * WORK: {@link IndexEmbedded#depth} = 1
 *
 * 20 Technical Manager____________
 * 21 Leasing Manager _____________|-18 Real estate director _
 *                                                            |
 * 22 Financial Analyst ___________                           |-1 John of England
 * 23 Internal Audit Manager ______|-19 Financial Director ___|
 * </pre>
 */
public class WorkDoneOnEntitiesTest extends SearchTestCase {
	
	private Session session = null;

	public void testEmployeesIndexingInDepth() throws Exception {
		List<WorkingPerson> result = search( session, "employees.name", "Technical Manager" );

		assertEquals( "Unexpected number of results", 1, result.size() );
		assertEquals(
			"Should be able to index field inside depth and in path",
			"Real estate director", result.get( 0 ).name
		);
	}

	public void testParentsIndexingInDepth() throws Exception {
		List<WorkingPerson> result = search( session, "parents.parents.name", "Empress Matilda" );

		assertEquals( "Unexpected number of results", 1, result.size() );
		assertEquals(
			"Should be able to index field inside depth and in path",
			"John of England", result.get( 0 ).name
		);
	}

	public void testNoWorkShouldBeExecutedOnPerson() throws Exception {
		renamePerson( session, 17, "Montford" );
		assertEquals( 0, countWorksDoneOnPersonId( 1 ) );
	}

	public void testWorkShouldBeExecutedOnPerson() throws Exception {
		renamePerson( session, 6, "William" );
		assertEquals( 1, countWorksDoneOnPersonId( 1 ) );
	}

	public void testNoWorkShouldBeExecutedOnEmployee() throws Exception {
		renamePerson( session, 23, "LM" );
		assertEquals( 0, countWorksDoneOnPersonId( 1 ) );
	}

	public void testWorkShouldBeExecutedOnEmployee() throws Exception {
		renamePerson( session, 19, "FM" );
		assertEquals( 1, countWorksDoneOnPersonId( 1 ) );
	}

	public void testShouldNotIndexParentsBeyondDepth() throws Exception {
		try {
			search( session, "parents.parents.parents.name", "Bertrade de Montfort" );
			fail( "Should not index a field if it is beyond the depth threshold" );
		}
		catch ( SearchException e ) {
		}
	}

	public void testShouldNotIndexEmployeesBeyondDepth() throws Exception {
		try {
			search( session, "employees.employees.name", "Techincal Manager" );
			fail( "Should not index a field if it is beyond the depth threshold" );
		}
		catch ( SearchException e ) {
		}
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		session = openSession();
		Transaction transaction = session.beginTransaction();
		WorkingPerson[] ps = new WorkingPerson[24];
		// array index starting from 1 to match ids of picture at http://en.wikipedia.org/wiki/John,_King_of_England
		ps[1] = new WorkingPerson( 1, "John of England" );
		ps[2] = new WorkingPerson( 2, "Henry II of England" );
		ps[3] = new WorkingPerson( 3, "Eleanor of Aquitaine" );
		ps[4] = new WorkingPerson( 4, "Geoffrey V  of Anjou" );
		ps[5] = new WorkingPerson( 5, "Empress Matilda" );
		ps[6] = new WorkingPerson( 6, "William X of Aquitaine" );
		ps[7] = new WorkingPerson( 7, "Aenor de Châtellerault" );
		ps[8] = new WorkingPerson( 8, "Fulk V of Anjou" );
		ps[9] = new WorkingPerson( 9, "Ermengarde of Maine" );
		ps[10] = new WorkingPerson( 10, "Henry I of England" );
		ps[11] = new WorkingPerson( 11, "Matilda of Scotland" );
		ps[12] = new WorkingPerson( 12, "William IX of Aquitaine" );
		ps[13] = new WorkingPerson( 13, "Philippa of Toulouse" );
		ps[14] = new WorkingPerson( 14, "Aimery I of Châttellerault" );
		ps[15] = new WorkingPerson( 15, "Dangereuse de L'Isle Bouchard" );
		ps[16] = new WorkingPerson( 16, "Fulk IV of Anjou" );
		ps[17] = new WorkingPerson( 17, "Bertrade de Montfort" );
		
		ps[18] = new WorkingPerson( 18, "Real estate director" );
		ps[19] = new WorkingPerson( 19, "Financial Director" );
		ps[20] = new WorkingPerson( 20, "Technical Manager" );
		ps[21] = new WorkingPerson( 21, "Leasing Manager" );
		ps[22] = new WorkingPerson( 22, "Financial Analyst" );
		ps[23] = new WorkingPerson( 23, "Internal Audit Manager" );

		ps[1].addParents( ps[2], ps[3] );
		ps[2].addParents( ps[4], ps[5] );
		ps[4].addParents( ps[8], ps[9] );
		ps[8].addParents( ps[16], ps[17] );

		ps[5].addParents( ps[10], ps[11] );

		ps[3].addParents( ps[6], ps[7] );
		ps[6].addParents( ps[12], ps[13] );
		ps[7].addParents( ps[14], ps[15] );

		ps[1].addEmployees( ps[18], ps[19] );
		
		ps[18].addEmployees( ps[20], ps[21] );
		ps[19].addEmployees( ps[22], ps[23] );

		for ( int i = 1; i < ps.length; i++ ) {
			session.save( ps[i] );
		}
		transaction.commit();
		LeakingLuceneBackend.reset();
	}

	@Override
	public void tearDown() throws Exception {
		session.clear();

		deleteAll( session, WorkingPerson.class );
		session.close();
		LeakingLuceneBackend.reset();
		super.tearDown();
	}

	private List<WorkingPerson> search(Session s, String field, String value) {
		FullTextSession session = Search.getFullTextSession( s );
		@SuppressWarnings("unchecked")
		List<WorkingPerson> result = session
			.createFullTextQuery( searchQuery( field, value, session ) )
			.list();
		return result;
	}

	private Query searchQuery(String field, String value, FullTextSession session) {
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder()
				.forEntity( WorkingPerson.class ).get();
		return queryBuilder.keyword().onField( field ).matching( value ).createQuery();
	}

	private void deleteAll(Session s, Class<?>... classes) {
		Transaction tx = s.beginTransaction();
		for ( Class<?> each : classes ) {
			List<?> list = s.createCriteria( each ).list();
			for ( Object object : list ) {
				s.delete( object );
			}
		}
		tx.commit();
	}

	private void renamePerson(Session s, Integer id, String newName) {
		Transaction transaction = s.beginTransaction();
		WorkingPerson person = (WorkingPerson) s.load( WorkingPerson.class, id );
		person.name = newName;
		transaction.commit();
	}

	private int countWorksDoneOnPersonId(Integer pk) {
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
		return new Class<?>[] { WorkingPerson.class };
	}
	
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() );
	}
	
}