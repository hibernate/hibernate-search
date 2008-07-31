// $Id$
package org.hibernate.search.test.analyzer;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.ScopedAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class AnalyzerTest extends SearchTestCase {

	public static final Logger log = LoggerFactory.getLogger( AnalyzerTest.class );

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
		QueryParser parser = new QueryParser( "id", new StandardAnalyzer() );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "entity:alarm" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "property:cat" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "field:energy" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "component.componentProperty:noise" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		s.delete( query.uniqueResult() );
		tx.commit();

		s.close();
	}

	public void testScopedAnalyzersAPI() throws Exception {
		FullTextSession session = Search.getFullTextSession( openSession() );
		SearchFactory searchFactory = session.getSearchFactory();
		ScopedAnalyzer analzyer = searchFactory.getAnalyzer( MyEntity.class );

		// explicitly get the global analyzer
		assertEquals( "Wrong analyzer", Test1Analyzer.class, analzyer.getGlobalAnalyzer().getClass() );

		// get the global analyzer by specifying a non existing field or null
		assertEquals( "Wrong analyzer", Test1Analyzer.class, analzyer.getAnalyzer("").getClass() );
		assertEquals( "Wrong analyzer", Test1Analyzer.class, analzyer.getAnalyzer(null).getClass() );
		
		// get a field analyzer
		assertEquals( "Wrong analyzer", Test1Analyzer.class, analzyer.getAnalyzer("entity").getClass() );
		assertEquals( "Wrong analyzer", Test2Analyzer.class, analzyer.getAnalyzer("property").getClass() );
		assertEquals( "Wrong analyzer", Test3Analyzer.class, analzyer.getAnalyzer("field").getClass() );
		assertEquals( "Wrong analyzer", Test4Analyzer.class, analzyer.getAnalyzer("component.componentProperty").getClass() );
		
		// get the field set
		Set<String> fields = new HashSet<String>();
		fields.add( "entity" );
		fields.add( "property" );
		fields.add( "field" );
		fields.add( "component.componentProperty" );
		assertEquals( "Wrong field set", fields, analzyer.getFields());
		
		// test border cases
		try {
			searchFactory.getAnalyzer( (Class) null );
		} catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}
		
		try {
			searchFactory.getAnalyzer( String.class );
		} catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}		
		
		session.close();
	}

	protected Class[] getMappings() {
		return new Class[] { MyEntity.class };
	}
}
