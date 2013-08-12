/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import junit.framework.Assert;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ReaderUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.LeakingLuceneBackend;
import org.hibernate.testing.SkipForDialect;

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
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@SkipForDialect(comment = "looks like a database deadlock", value = org.hibernate.dialect.SybaseASE15Dialect.class, jiraKey = "HSEARCH-1107")
public class WorkDoneOnEntitiesTest extends SearchTestCase {

	private Session session = null;

	public void testEmployeesIndexingInDepth() throws Exception {
		List<WorkingPerson> result = search( session, "employees.name", "Technical Manager" );

		assertEquals( "Unexpected number of results", 1, result.size() );
		assertEquals(
			"Should be able to index field inside depth and in path",
			"Real estate director", result.get( 0 ).name
		);
		checkRawIndexFields();
	}

	public void testParentsIndexingInDepth() throws Exception {
		List<WorkingPerson> result = search( session, "parents.parents.name", "Empress Matilda" );

		assertEquals( "Unexpected number of results", 1, result.size() );
		assertEquals(
			"Should be able to index field inside depth and in path",
			"John of England", result.get( 0 ).name
		);

		result = search( session, "parents.parents.parents.name", "Ermengarde of Maine" );
		assertEquals( "Unexpected number of results", 1, result.size() );
		checkRawIndexFields();
	}

	public void testNoWorkShouldBeExecutedOnPerson() throws Exception {
		renamePerson( session, 17, "Montford" );
		checkRawIndexFields();
		assertEquals( 0, countWorksDoneOnPersonId( 1 ) );
	}

	public void testWorkShouldBeExecutedOnPerson() throws Exception {
		renamePerson( session, 6, "William" );
		checkRawIndexFields();
		assertEquals( 1, countWorksDoneOnPersonId( 1 ) );
	}

	public void testNoWorkShouldBeExecutedOnEmployee() throws Exception {
		renamePerson( session, 23, "LM" );
		checkRawIndexFields();
		assertEquals( 0, countWorksDoneOnPersonId( 1 ) );
	}

	public void testWorkShouldBeExecutedOnEmployee() throws Exception {
		renamePerson( session, 19, "FM" );
		checkRawIndexFields();
		assertEquals( 1, countWorksDoneOnPersonId( 1 ) );
	}

	public void testShouldNotIndexParentsBeyondDepth() throws Exception {
		try {
			// fails only if DSL fails:
			search( session, "parents.parents.parents.parents.name", "Bertrade de Montfort" );
			fail( "Should not index a field if it is beyond the depth threshold" );
		}
		catch (SearchException e) {
		}
		checkRawIndexFields();
	}

	public void testShouldNotIndexBeyondMixedPathDepth() throws Exception {
		try {
			// fails only if DSL fails:
			search( session, "parents.employees.employees.name", "Techincal Manager" );
			fail( "Should not index a field if it is beyond the depth threshold, considering minimum depth along paths" );
		}
		catch (SearchException e) {
		}
		checkRawIndexFields();
	}

	public void testShouldNotIndexEmployeesBeyondDepth() throws Exception {
		try {
			search( session, "employees.employees.name", "Techincal Manager" );
			fail( "Should not index a field if it is beyond the depth threshold" );
		}
		catch (SearchException e) {
		}
		checkRawIndexFields();
	}

	private void checkRawIndexFields() {
		// check raw index as well:
		Assert.assertTrue( indexContainsField( "name" ) );
		Assert.assertTrue( indexContainsField( "employees.name" ) );
		Assert.assertTrue( indexContainsField( "parents.name" ) );
		Assert.assertTrue( indexContainsField( "parents.parents.name" ) );
		Assert.assertTrue( indexContainsField( "parents.parents.parents.name" ) );
		Assert.assertTrue( indexContainsField( "parents.employees.name" ) );
		Assert.assertTrue( indexContainsField( "parents.parents.employees.name" ) );
		Assert.assertFalse( indexContainsField( "employees.employees.name" ) );
		Assert.assertFalse( indexContainsField( "employees.parents.name" ) );
		Assert.assertFalse( indexContainsField( "parents.employees.employees.name" ) );
		Assert.assertFalse( indexContainsField( "parents.parents.parents.employees.name" ) );
	}

	private boolean indexContainsField(String fieldName) {
		IndexReaderAccessor indexReaderAccessor = getSearchFactory().getIndexReaderAccessor();
		IndexReader indexReader = indexReaderAccessor.open( WorkingPerson.class );
		try {
			return ReaderUtil.getIndexedFields( indexReader ).contains( fieldName );
		}
		finally {
			indexReaderAccessor.close( indexReader );
		}
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		session = openSession();
		Transaction transaction = session.beginTransaction();
		WorkingPerson[] ps = new WorkingPerson[27];
		// array index starting from 1 to match ids of picture at http://en.wikipedia.org/wiki/John,_King_of_England
		ps[1] = new WorkingPerson( 1, "John of England" );
		ps[2] = new WorkingPerson( 2, "Henry II of England" );
		ps[3] = new WorkingPerson( 3, "Eleanor of Aquitaine" );
		ps[4] = new WorkingPerson( 4, "Geoffrey V of Anjou" );
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
		ps[24] = new WorkingPerson( 24, "Slave of Henry II" );
		ps[25] = new WorkingPerson( 25, "Slave of Geoffrey V" );
		ps[26] = new WorkingPerson( 26, "Assistant of Slave of Geoffrey V" );

		ps[1].addParents( ps[2], ps[3] );
		ps[2].addParents( ps[4], ps[5] );
		ps[4].addParents( ps[8], ps[9] );
		ps[8].addParents( ps[16], ps[17] );

		ps[5].addParents( ps[10], ps[11] );

		ps[3].addParents( ps[6], ps[7] );
		ps[6].addParents( ps[12], ps[13] );
		ps[7].addParents( ps[14], ps[15] );

		ps[1].addEmployees( ps[18], ps[19] );
		ps[2].addEmployees( ps[24] );
		ps[5].addEmployees( ps[25] );
		ps[25].addEmployees( ps[26] );

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

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() );
	}

}
