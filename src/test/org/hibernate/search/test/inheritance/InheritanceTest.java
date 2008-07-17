//$Id$
package org.hibernate.search.test.inheritance;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.Transaction;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.index.Term;

import java.util.List;

/**
 * @author Emmanuel Bernard
 */
public class InheritanceTest extends SearchTestCase {

	public void testInheritance() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Animal a = new Animal();
        a.setName("Shark Jr");
        s.save( a );
        Mammal m = new Mammal();
        m.setMammalNbr(2);
        m.setName("Elephant Jr");
		m.setWeight( 400 );
		s.save(m);
		tx.commit();//post commit events for lucene
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser("name", new StopAnalyzer() );

		Query query;
		org.hibernate.Query hibQuery;

        query = parser.parse( "Elephant" );
		hibQuery = s.createFullTextQuery( query, Mammal.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query subclass by superclass attribute", 1, result.size() );

        query = parser.parse( "mammalNbr:[2 TO 2]" );
		hibQuery = s.createFullTextQuery( query, Animal.class, Mammal.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query subclass by subclass attribute", 1, result.size() );

        query = parser.parse( "Jr" );
		hibQuery = s.createFullTextQuery( query, Animal.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query filtering on superclass return mapped subclasses", 2, result.size() );

		query = new RangeQuery( new Term( "weight", "00200" ), null, true);
		hibQuery = s.createFullTextQuery( query, Animal.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query on non @Indexed superclass property", 1, result.size() );

		for (Object managedEntity : result) {
            s.delete(managedEntity);
        }
        tx.commit();
		s.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
                Animal.class,
                Mammal.class
        };
	}
}
