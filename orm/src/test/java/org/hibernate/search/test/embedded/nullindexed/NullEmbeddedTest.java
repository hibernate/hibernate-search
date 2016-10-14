/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.nullindexed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Davide D'Alto
 */
public class NullEmbeddedTest extends SearchTestBase {

	@Test
	public void testEmbeddedNullNotIndexedQuery() throws Exception {
		Man withoutPuppies = new Man( "Davide" );
		withoutPuppies.setPartner( null );

		Pet dog = new Pet( "dog" );
		dog.setPuppies( null );
		withoutPuppies.setPet( dog );

		Man wihKittens = new Man( "Omar" );

		Pet cat = new Pet( "cat" );
		wihKittens.setPet( cat );
		Puppy kittenOne = new Puppy( "kitten one" );
		Puppy KittenTwo = new Puppy( "kitten two" );
		cat.addPuppy( kittenOne ).addPuppy( KittenTwo );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( withoutPuppies );
		s.persist( wihKittens );
		s.persist( dog );
		s.persist( cat );
		s.persist( kittenOne );
		s.persist( KittenTwo );
		tx.commit();

		try {
			findNullsFor( s, "partner", "indexAsNull not set" );
			fail( "Embedded null field should not exists for field without indexAsNull property" );
		}
		catch (SearchException e) {
			// Succeded: indexAsNull not specified so the field is not created
		}

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Man.class, withoutPuppies.getId() ) );
		s.delete( s.get( Man.class, wihKittens.getId() ) );
		s.delete( s.get( Pet.class, dog.getId() ) );
		s.delete( s.get( Pet.class, cat.getId() ) );
		s.delete( s.get( Puppy.class, kittenOne.getId() ) );
		s.delete( s.get( Puppy.class, KittenTwo.getId() ) );
		tx.commit();

		s.close();
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2389 Support indexNullAs for @IndexedEmbedded applied on objects with Elasticsearch
	public void testNestedEmbeddedNullIndexing() throws Exception {
		Man withPet = new Man( "Davide" );

		Pet dog = new Pet( "dog" );
		dog.setPuppies( null );
		withPet.setPet( dog );

		Man withPuppies = new Man( "Omar" );

		Pet cat = new Pet( "cat" );
		withPuppies.setPet( cat );
		Puppy puppy1 = new Puppy( "puppy one" );
		Puppy puppy2 = new Puppy( "puppy two" );
		cat.addPuppy( puppy1 ).addPuppy( puppy2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( withPet );
		s.persist( withPuppies );
		s.persist( dog );
		s.persist( cat );
		s.persist( puppy1 );
		s.persist( puppy2 );
		tx.commit();

		List<Man> result = findNullsFor( s, "pet.pups", "_null_" );

		assertEquals( "Wrong number of results found", 1, result.size() );
		assertEquals( "Wrong result returned", withPet, result.get( 0 ) );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Man.class, withPet.getId() ) );
		s.delete( s.get( Man.class, withPuppies.getId() ) );
		s.delete( s.get( Pet.class, dog.getId() ) );
		s.delete( s.get( Pet.class, cat.getId() ) );
		s.delete( s.get( Puppy.class, puppy1.getId() ) );
		s.delete( s.get( Puppy.class, puppy2.getId() ) );
		tx.commit();

		s.close();
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2389 Support indexNullAs for @IndexedEmbedded applied on objects with Elasticsearch
	public void testEmbeddedNullIndexing() throws Exception {
		Man me = new Man( "Davide" );
		Pet dog = new Pet( "dog" );
		me.setPet( null );

		Man someoneElse = new Man( "Omar" );

		Pet cat = new Pet( "cat" );
		someoneElse.setPet( cat );
		Puppy puppy1 = new Puppy( "puppy one" );
		Puppy puppy2 = new Puppy( "puppy two" );
		cat.addPuppy( puppy1 ).addPuppy( puppy2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( me );
		s.persist( someoneElse );
		s.persist( dog );
		s.persist( cat );
		s.persist( puppy1 );
		s.persist( puppy2 );
		tx.commit();

		List<Man> result = findNullsFor( getSession(), "pet", Man.NO_PET );
		assertEquals( "Wrong number of results found", 1, result.size() );
		assertEquals( "Wrong result returned", me, result.get( 0 ) );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Man.class, me.getId() ) );
		s.delete( s.get( Man.class, someoneElse.getId() ) );
		s.delete( s.get( Pet.class, dog.getId() ) );
		s.delete( s.get( Pet.class, cat.getId() ) );
		s.delete( s.get( Puppy.class, puppy1.getId() ) );
		s.delete( s.get( Puppy.class, puppy2.getId() ) );
		tx.commit();

		s.close();
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2389 Support indexNullAs for @IndexedEmbedded applied on objects with Elasticsearch
	public void testNestedEmbeddedNullElementCollectionIndexing() throws Exception {
		Man withPetWithoutTricks = new Man( "Davide" );

		Pet cat = new Pet( "cat" );
		cat.setTricks( null );
		withPetWithoutTricks.setPet( cat );

		Man withPetWithTricks = new Man( "Omar" );

		Pet dog = new Pet( "dog" );
		withPetWithTricks.setPet( dog );
		Trick trick1 = new Trick( "sit", "bone" );
		Trick trick2 = new Trick( "high five", "steak" );
		dog.addTrick( trick1 ).addTrick( trick2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( withPetWithoutTricks );
		s.persist( withPetWithTricks );
		s.persist( cat );
		s.persist( dog );
		tx.commit();

		List<Man> result = findNullsFor( s, "pet.tricks_", "_null_" );

		assertEquals( "Wrong number of results found", 1, result.size() );
		assertEquals( "Wrong result returned", withPetWithoutTricks, result.get( 0 ) );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Man.class, withPetWithoutTricks.getId() ) );
		s.delete( s.get( Man.class, withPetWithTricks.getId() ) );
		s.delete( s.get( Pet.class, cat.getId() ) );
		s.delete( s.get( Pet.class, dog.getId() ) );
		tx.commit();

		s.close();
	}


	private List<Man> findNullsFor(Session s, String fieldName, String value) {
		FullTextSession session = Search.getFullTextSession( s );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( Man.class ).get();
		Query query = queryBuilder.keyword().onField( fieldName ).ignoreAnalyzer().matching( value ).createQuery();
		@SuppressWarnings("unchecked")
		List<Man> result = session.createFullTextQuery( query ).list();
		return result;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Man.class, Pet.class, Puppy.class, Woman.class };
	}
}
