/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.depth;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.backend.LeakingLuceneBackend;
import org.hibernate.testing.SkipForDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <pre>
 * FAMILY: {@link org.hibernate.search.annotations.IndexedEmbedded#depth()} = 2
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
 * WORK: {@link org.hibernate.search.annotations.IndexedEmbedded#depth()#depth} = 1
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
public class WorkDoneOnEntitiesTest extends SearchTestBase {

	@Test
	public void testEmployeesIndexingInDepth() throws Exception {
		List<WorkingPerson> result = search( "employees.name", "Technical Manager" );

		assertEquals( "Unexpected number of results", 1, result.size() );
		assertEquals(
			"Should be able to index field inside depth and in path",
			"Real estate director", result.get( 0 ).name
		);
		checkRawIndexFields();
	}

	@Test
	public void testParentsIndexingInDepth() throws Exception {
		List<WorkingPerson> result = search( "parents.parents.name", "Empress Matilda" );

		assertEquals( "Unexpected number of results", 1, result.size() );
		assertEquals(
			"Should be able to index field inside depth and in path",
			"John of England", result.get( 0 ).name
		);

		result = search( "parents.parents.parents.name", "Ermengarde of Maine" );
		assertEquals( "Unexpected number of results", 1, result.size() );
		checkRawIndexFields();
	}

	@Test
	public void testNoWorkShouldBeExecutedOnPerson() throws Exception {
		renamePerson( 17, "Montford" );
		checkRawIndexFields();
		assertEquals( 0, countWorksDoneOnPersonId( 1 ) );
	}

	@Test
	public void testWorkShouldBeExecutedOnPerson() throws Exception {
		renamePerson( 6, "William" );
		checkRawIndexFields();
		assertEquals( 1, countWorksDoneOnPersonId( 1 ) );
	}

	@Test
	public void testNoWorkShouldBeExecutedOnEmployee() throws Exception {
		renamePerson( 23, "LM" );
		checkRawIndexFields();
		assertEquals( 0, countWorksDoneOnPersonId( 1 ) );
	}

	@Test
	public void testWorkShouldBeExecutedOnEmployee() throws Exception {
		renamePerson( 19, "FM" );
		checkRawIndexFields();
		assertEquals( 1, countWorksDoneOnPersonId( 1 ) );
	}

	@Test
	public void testShouldNotIndexParentsBeyondDepth() throws Exception {
		try {
			// fails only if DSL fails:
			search( "parents.parents.parents.parents.name", "Bertrade de Montfort" );
			fail( "Should not index a field if it is beyond the depth threshold" );
		}
		catch (SearchException e) {
		}
		checkRawIndexFields();
	}

	@Test
	public void testShouldNotIndexBeyondMixedPathDepth() throws Exception {
		try {
			// fails only if DSL fails:
			search( "parents.employees.employees.name", "Techincal Manager" );
			fail( "Should not index a field if it is beyond the depth threshold, considering minimum depth along paths" );
		}
		catch (SearchException e) {
		}
		checkRawIndexFields();
	}

	@Test
	public void testShouldNotIndexEmployeesBeyondDepth() throws Exception {
		try {
			search( "employees.employees.name", "Techincal Manager" );
			fail( "Should not index a field if it is beyond the depth threshold" );
		}
		catch (SearchException e) {
		}
		checkRawIndexFields();
	}

	private void checkRawIndexFields() throws IOException {
		// check raw index as well:
		assertTrue( indexContainsField( "name" ) );
		assertTrue( indexContainsField( "employees.name" ) );
		assertTrue( indexContainsField( "parents.name" ) );
		assertTrue( indexContainsField( "parents.parents.name" ) );
		assertTrue( indexContainsField( "parents.parents.parents.name" ) );
		assertTrue( indexContainsField( "parents.employees.name" ) );
		assertTrue( indexContainsField( "parents.parents.employees.name" ) );
		assertFalse( indexContainsField( "employees.employees.name" ) );
		assertFalse( indexContainsField( "employees.parents.name" ) );
		assertFalse( indexContainsField( "parents.employees.employees.name" ) );
		assertFalse( indexContainsField( "parents.parents.parents.employees.name" ) );
	}

	private boolean indexContainsField(String fieldName) throws IOException {
		IndexReaderAccessor indexReaderAccessor = getSearchFactory().getIndexReaderAccessor();
		IndexReader indexReader = indexReaderAccessor.open( WorkingPerson.class );
		try {
			for ( AtomicReaderContext leave : indexReader.leaves() ) {
				if ( leave.reader().terms( fieldName ) != null ) {
					return true;
				}
			}
			return false;
		}
		finally {
			indexReaderAccessor.close( indexReader );
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
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
	@After
	public void tearDown() throws Exception {
		LeakingLuceneBackend.reset();
		super.tearDown();
	}

	private List<WorkingPerson> search(String field, String value) {
		FullTextSession session = Search.getFullTextSession( getSession() );
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

	private void renamePerson(Integer id, String newName) {
		Transaction transaction = getSession().beginTransaction();
		WorkingPerson person = (WorkingPerson) getSession().load( WorkingPerson.class, id );
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
