/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.junit.jupiter.api.Test;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class InheritanceTest extends SearchTestBase {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Test
	void testSearchUnindexClass() throws Exception {
		createTestData();

		QueryParser parser = new QueryParser( "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		try {
			org.hibernate.query.Query hibQuery = s.createFullTextQuery( query, String.class );
			hibQuery.list();
			tx.commit();
			fail();
		}
		catch (SearchException e) {
			log.debug( "success" );
			tx.rollback();
		}

		tx = s.beginTransaction();
		org.hibernate.query.Query hibQuery = s.createFullTextQuery( query, Mammal.class );
		assertItsTheElephant( hibQuery.list() );
		tx.commit();

		s.close();
	}

	@Test
	void testInheritance() throws Exception {
		createTestData();

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant" );
		org.hibernate.query.Query hibQuery = s.createFullTextQuery( query, Mammal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "Elephant" );
		hibQuery = s.createFullTextQuery( query );
		assertItsTheElephant( hibQuery.list() );

		query = IntPoint.newExactQuery( "hasSweatGlands", 0 );
		hibQuery = s.createFullTextQuery( query, Animal.class, Mammal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "Elephant OR White Pointer" );
		hibQuery = s.createFullTextQuery( query, Being.class );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query filtering on superclass return mapped subclasses" ).hasSize( 2 );

		query = IntPoint.newRangeQuery( "weight", 4000, 5000 );
		hibQuery = s.createFullTextQuery( query, Animal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "Elephant" );
		hibQuery = s.createFullTextQuery( query, Being.class );
		assertItsTheElephant( hibQuery.list() );

		tx.commit();
		s.close();
	}


	@Test
	void testPolymorphicQueries() throws Exception {
		createTestData();

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant" );

		org.hibernate.query.Query hibQuery = s.createFullTextQuery( query, Mammal.class );
		assertItsTheElephant( hibQuery.list() );

		hibQuery = s.createFullTextQuery( query, Animal.class );
		assertItsTheElephant( hibQuery.list() );

		hibQuery = s.createFullTextQuery( query, Being.class );
		assertItsTheElephant( hibQuery.list() );

		hibQuery = s.createFullTextQuery( query, Object.class );
		assertItsTheElephant( hibQuery.list() );

		hibQuery = s.createFullTextQuery( query, Serializable.class );
		assertItsTheElephant( hibQuery.list() );

		hibQuery = s.createFullTextQuery(
				query, Mammal.class, Animal.class, Being.class, Object.class, Serializable.class
		);
		assertItsTheElephant( hibQuery.list() );

		tx.commit();
		s.close();
	}

	@Test
	void testSubclassInclusion() {
		createTestData();

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Query query = IntPoint.newExactQuery( "numberOfEggs", 2 );
		org.hibernate.query.Query hibQuery = s.createFullTextQuery( query, Eagle.class );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Wrong number of hits. There should be two birds." ).hasSize( 1 );

		query = IntPoint.newExactQuery( "numberOfEggs", 2 );
		hibQuery = s.createFullTextQuery( query, Bird.class );
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Wrong number of hits. There should be two birds." ).hasSize( 2 );

		hibQuery = s.createFullTextQuery( query, Mammal.class );
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Wrong number of hits. There should be two birds." ).isEmpty();

		try {
			hibQuery = s.createFullTextQuery( query, String.class );
			hibQuery.list();
			fail();
		}
		catch (SearchException e) {
			log.debug( "success" );
		}

		tx.commit();
		s.close();
	}

	/**
	 * Tests that purging the index of a class also purges the index of the subclasses. See also HSEARCH-262.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	void testPurgeIndex() throws Exception {
		createTestData();
		FullTextSession s = Search.getFullTextSession( openSession() );

		Transaction tx = s.beginTransaction();
		assertNumberOfAnimals( s, 5 );
		tx.commit();

		tx = s.beginTransaction();
		s.purgeAll( Serializable.class );
		tx.commit();

		tx = s.beginTransaction();
		assertNumberOfAnimals( s, 3 );
		tx.commit();

		tx = s.beginTransaction();
		s.purgeAll( Bird.class );
		tx.commit();

		tx = s.beginTransaction();
		assertNumberOfAnimals( s, 1 );
		tx.commit();

		tx = s.beginTransaction();
		s.purgeAll( Object.class );
		tx.commit();

		tx = s.beginTransaction();
		assertNumberOfAnimals( s, 0 );
		tx.commit();

		s.close();
	}

	/**
	 * Tests that purging the an uninexed class triggers an exception.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	void testPurgeUnIndexClass() throws Exception {
		createTestData();
		FullTextSession s = Search.getFullTextSession( openSession() );

		Transaction tx = s.beginTransaction();
		assertNumberOfAnimals( s, 5 );
		tx.commit();

		tx = s.beginTransaction();
		try {
			s.purgeAll( String.class );
			tx.commit();
			fail();
		}
		catch (SearchException e) {
			log.debug( "Success" );
		}
		s.close();
	}

	private void assertNumberOfAnimals(FullTextSession s, int count) throws Exception {
		QueryParser parser = new QueryParser( "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant OR White Pointer OR Chimpanzee OR Dove or Eagle" );
		List result = s.createFullTextQuery( query, Animal.class ).list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Wrong number of hits. There should be one elephant and one shark." ).hasSize( count );
	}

	private void createTestData() {
		try ( FullTextSession s = Search.getFullTextSession( openSession() ) ) {
			Transaction tx = s.beginTransaction();

			Fish shark = new Fish();
			shark.setName( "White Pointer" );
			shark.setNumberOfDorsalFins( 2 );
			shark.setWeight( 1500 );
			s.save( shark );

			Mammal elephant = new Mammal();
			elephant.setName( "Elephant" );
			elephant.setHasSweatGlands( false );
			elephant.setWeight( 4500 );
			s.save( elephant );

			Mammal chimp = new Mammal();
			chimp.setName( "Chimpanzee" );
			chimp.setHasSweatGlands( true );
			chimp.setWeight( 50 );
			s.save( chimp );

			Bird dove = new Bird();
			dove.setName( "Dove" );
			dove.setNumberOfEggs( 2 );
			s.save( dove );

			Eagle eagle = new Eagle();
			eagle.setName( "Bald Eagle" );
			eagle.setNumberOfEggs( 2 );
			eagle.setWingType( Eagle.WingType.BROAD );
			s.save( eagle );

			tx.commit();
		}
	}

	private void assertItsTheElephant(List result) {
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Wrong number of results" ).hasSize( 1 );
		assertThat( result.get( 0 ) ).as( "Wrong result type" ).isInstanceOf( Mammal.class );
		Mammal mammal = (Mammal) result.get( 0 );
		assertThat( mammal.getName() ).as( "Wrong animal name" ).isEqualTo( "Elephant" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Animal.class,
				Mammal.class,
				Fish.class,
				Bird.class,
				Eagle.class
		};
	}
}
