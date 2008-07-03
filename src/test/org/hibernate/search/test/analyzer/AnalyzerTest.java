//$Id$
package org.hibernate.search.test.analyzer;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.Transaction;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public class AnalyzerTest extends SearchTestCase {

	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setEntity( "Entity" );
		en.setField( "Field" );
		en.setProperty( "Property" );
		en.setComponent( new MyComponent() );
		en.getComponent().setComponentProperty( "component property" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( en );
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser("id", new StandardAnalyzer() );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "entity:alarm");
		FullTextQuery query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "property:cat");
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "field:energy");
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "component.componentProperty:noise");
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		s.delete( query.uniqueResult() );
		tx.commit();

		s.close();

	}

	protected Class[] getMappings() {
		return new Class[] {
				MyEntity.class
		};
	}
}
