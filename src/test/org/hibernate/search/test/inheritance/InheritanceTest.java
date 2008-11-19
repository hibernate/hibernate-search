//$Id$
package org.hibernate.search.test.inheritance;

import java.util.List;
import java.io.Serializable;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class InheritanceTest extends SearchTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testInheritance() throws Exception {
		createTestData();

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "name", new StopAnalyzer() );
		Query query = parser.parse( "Elephant" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Mammal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "hasSweatGlands:false" );
		hibQuery = s.createFullTextQuery( query, Animal.class, Mammal.class );
		assertItsTheElephant( hibQuery.list() );

		query = parser.parse( "Elephant OR White Pointer" );
		hibQuery = s.createFullTextQuery( query, Being.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query filtering on superclass return mapped subclasses", 2, result.size() );

		query = new RangeQuery( new Term( "weight", "04000" ), new Term( "weight", "05000" ), true );
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
		QueryParser parser = new QueryParser( "name", new StopAnalyzer() );
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

		hibQuery = s.createFullTextQuery( query, Mammal.class, Animal.class, Being.class, Object.class, Serializable.class );
		assertItsTheElephant( hibQuery.list() );

		tx.commit();
		s.close();
	}

	private void assertItsTheElephant(List result) {
		assertNotNull( result );
		assertEquals( "Wrong number of results", 1, result.size() );
		assertTrue( "Wrong result type", result.get( 0 ) instanceof Mammal );
		Mammal mammal = ( Mammal ) result.get( 0 );
		assertEquals( "Wrong animal name", "Elephant", mammal.getName() );
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
		QueryParser parser = new QueryParser( "name", new StopAnalyzer() );

		Query query = parser.parse( "Elephant OR White Pointer OR Chimpanzee" );
		List result = s.createFullTextQuery( query, Animal.class ).list();
		assertNotNull( result );
		assertEquals( "Wrong number of hits. There should be one elephant and one shark.", 3, result.size() );

		s.purgeAll( Animal.class );
		tx.commit();
		tx = s.beginTransaction();
		result = s.createFullTextQuery( query, Animal.class ).list();
		assertNotNull( result );
		assertEquals(
				"Wrong number of hits. Purging the Animal class should also purge the Mammals", 0, result.size()
		);

		tx.commit();
		s.close();
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

		tx.commit();
		s.clear();
	}

	protected Class[] getMappings() {
		return new Class[] {
				Animal.class,
				Mammal.class,
				Fish.class
		};
	}
}
