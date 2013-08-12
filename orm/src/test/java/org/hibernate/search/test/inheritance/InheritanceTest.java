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
package org.hibernate.search.test.inheritance;

import java.util.List;
import java.io.Serializable;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Emmanuel Bernard
 */
public class InheritanceTest extends SearchTestCase {

	private static final Log log = LoggerFactory.make();

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	public void testSearchUnindexClass() throws Exception {
		createTestData();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		try {
			org.hibernate.Query hibQuery = s.createFullTextQuery( query, String.class );
			hibQuery.list();
			tx.commit();
			fail();
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
			tx.rollback();
		}

		tx = s.beginTransaction();
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Mammal.class );
		assertItsTheElephant( hibQuery.list() );
		tx.commit();

		s.close();
	}

	public void testInheritance() throws Exception {
		createTestData();

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Mammal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "Elephant" );
		hibQuery = s.createFullTextQuery( query);
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "hasSweatGlands:false" );
		hibQuery = s.createFullTextQuery( query, Animal.class, Mammal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "Elephant OR White Pointer" );
		hibQuery = s.createFullTextQuery( query, Being.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query filtering on superclass return mapped subclasses", 2, result.size() );

		query = new TermRangeQuery( "weight", "04000", "05000", true, true );
		hibQuery = s.createFullTextQuery( query, Animal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "Elephant" );
		hibQuery = s.createFullTextQuery( query, Being.class );
		assertItsTheElephant( hibQuery.list() );

		tx.commit();
		s.close();
	}


	public void testPolymorphicQueries() throws Exception {
		createTestData();

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant" );

		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Mammal.class );
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

	public void testSubclassInclusion() throws Exception {
		createTestData();

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Query query = new TermQuery( new Term( "numberOfEggs", "2" ) );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Eagle.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of hits. There should be two birds.", 1, result.size() );

		query = new TermQuery( new Term( "numberOfEggs", "2" ) );
		hibQuery = s.createFullTextQuery( query, Bird.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of hits. There should be two birds.", 2, result.size() );

		query = new TermQuery( new Term( "numberOfEggs", "2" ) );
		hibQuery = s.createFullTextQuery( query, Mammal.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of hits. There should be two birds.", 0, result.size() );

		try {
			query = new TermQuery( new Term( "numberOfEggs", "2" ) );
			hibQuery = s.createFullTextQuery( query, String.class );
			hibQuery.list();
			fail();
		}
		catch (IllegalArgumentException iae) {
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
	public void testPurgeIndex() throws Exception {
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
	public void testPurgeUnIndexClass() throws Exception {
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
		catch (IllegalArgumentException iae) {
			log.debug( "Success" );
		}
		s.close();
	}

	private void assertNumberOfAnimals(FullTextSession s, int count) throws Exception {
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.stopAnalyzer );
		Query query = parser.parse( "Elephant OR White Pointer OR Chimpanzee OR Dove or Eagle" );
		List result = s.createFullTextQuery( query, Animal.class ).list();
		assertNotNull( result );
		assertEquals( "Wrong number of hits. There should be one elephant and one shark.", count, result.size() );
	}

	private void createTestData() {
		FullTextSession s = Search.getFullTextSession( openSession() );
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
		eagle.setWingYype( Eagle.WingType.BROAD );
		s.save( eagle );

		tx.commit();
		s.clear();
	}

	private void assertItsTheElephant(List result) {
		assertNotNull( result );
		assertEquals( "Wrong number of results", 1, result.size() );
		assertTrue( "Wrong result type", result.get( 0 ) instanceof Mammal );
		Mammal mammal = (Mammal) result.get( 0 );
		assertEquals( "Wrong animal name", "Elephant", mammal.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Animal.class,
				Mammal.class,
				Fish.class,
				Bird.class,
				Eagle.class
		};
	}
}
