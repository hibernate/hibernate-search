/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path;

import java.util.List;

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * John of England Genealogy
 *
 * <pre>
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
 * @author Davide D'Alto
 */
public class PathEmbeddedDepthTest extends SearchTestBase {

	private Session session = null;

	@Test
	public void testShouldIndexFieldInPath() throws Exception {
		List<Human> result = search( session, "parents.parents.parents.name", "Philippa" );

		Assert.assertEquals( 1, result.size() );
		Assert.assertEquals(
				"Should be able to index a field in path regarding the depth",
				"John of England", result.get( 0 ).getFullname() );
	}

	@Test
	public void testIndexFieldIfInsideDepth() throws Exception {
		List<Human> result = search( session, "parents.parents.name", "Empress" );

		Assert.assertEquals( 1, result.size() );
		Assert.assertEquals(
				"Should be able to index field inside depth and in path",
				"John of England", result.get( 0 ).getFullname() );
	}

	@Test
	public void testShouldNotIndexFieldOutsidePathAndOverDepth() throws Exception {
		try {
			search( session, "parents.parents.parents.surname", "de Montfort" );
			fail( "Shoudl not index a field if not in path and over the depth threshold" );
		}
		catch (SearchException e) {
		}
	}

	@Test
	public void testShouldIndexFieldNotInPathButInsideDepthThreshold() throws Exception {
		List<Human> result = search( session, "parents.parents.surname", "de Châtellerault" );

		Assert.assertEquals( 1, result.size() );
		Assert.assertEquals( "Should be able to index a field if is inside the depth threshold even if not in path",
				"John of England", result.get( 0 ).getFullname() );
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		session = openSession();
		Transaction transaction = session.beginTransaction();
		Human[] ps = new Human[18];
		// array index starting from 1 to match ids of picture at http://en.wikipedia.org/wiki/John,_King_of_England
		ps[1] = new Human( "John", "of England" );
		ps[2] = new Human( "Henry II", "of England" );
		ps[3] = new Human( "Eleanor", "of Aquitaine" );
		ps[4] = new Human( "Geoffrey V", " of Anjou" );
		ps[5] = new Human( "Empress", "Matilda" );
		ps[6] = new Human( "William X", "of Aquitaine" );
		ps[7] = new Human( "Aenor", "de Châtellerault" );
		ps[8] = new Human( "Fulk V", "of Anjou" );
		ps[9] = new Human( "Ermengarde", "of Maine" );
		ps[10] = new Human( "Henry I", "of England" );
		ps[11] = new Human( "Matilda", "of Scotland" );
		ps[12] = new Human( "William IX", "of Aquitaine" );
		ps[13] = new Human( "Philippa", "of Toulouse" );
		ps[14] = new Human( "Aimery I", "of Châttellerault" );
		ps[15] = new Human( "Dangereuse", "de L'Isle Bouchard" );
		ps[16] = new Human( "Fulk IV", "of Anjou" );
		ps[17] = new Human( "Bertrade", "de Montfort" );

		ps[1].addParents( ps[2], ps[3] );
		ps[2].addParents( ps[4], ps[5] );
		ps[4].addParents( ps[8], ps[9] );
		ps[8].addParents( ps[16], ps[17] );

		ps[5].addParents( ps[10], ps[11] );

		ps[3].addParents( ps[6], ps[7] );
		ps[6].addParents( ps[12], ps[13] );
		ps[7].addParents( ps[14], ps[15] );

		for ( int i = 1; i < 18; i++ ) {
			session.save( ps[i] );
		}
		transaction.commit();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		session.clear();

		deleteAll( session, Human.class );
		session.close();
		super.tearDown();
	}

	private List<Human> search(Session s, String field, String value) {
		FullTextSession session = Search.getFullTextSession( s );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( Human.class ).get();
		Query query = queryBuilder.keyword().onField( field ).matching( value ).createQuery();
		@SuppressWarnings("unchecked")
		List<Human> result = session.createFullTextQuery( query ).list();
		return result;
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

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Human.class };
	}
}
