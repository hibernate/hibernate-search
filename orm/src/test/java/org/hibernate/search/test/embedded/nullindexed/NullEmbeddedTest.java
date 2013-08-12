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
package org.hibernate.search.test.embedded.nullindexed;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Davide D'Alto
 */
public class NullEmbeddedTest extends SearchTestCase {

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

	public void testNestedEmebeddedNullIndexing() throws Exception {
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

		List<Man> result = findNullsFor( s, "pet.puppies", "_null_" );

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

	private List<Man> findNullsFor(Session s, String fieldName, String value) {
		FullTextSession session = Search.getFullTextSession( s );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( Man.class ).get();
		Query query = queryBuilder.keyword().onField( fieldName ).ignoreAnalyzer().matching( value ).createQuery();
		@SuppressWarnings("unchecked")
		List<Man> result = session.createFullTextQuery( query ).list();
		return result;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Man.class, Pet.class, Puppy.class, Woman.class };
	}
}
