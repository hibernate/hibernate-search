package org.hibernate.search.test.analyzer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.LoggerFactory;

/**
 * Test for http://opensource.atlassian.com/projects/hibernate/browse/HSEARCH-263
 * @author Sanne Grinovero
 */
public class DoubleAnalyzerTest extends SearchTestCase {

	public static final Logger log = LoggerFactory.make();

	protected Class[] getMappings() {
		return new Class[] { MyEntity.class, AlarmEntity.class };
	}

	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setEntity( "anyNotNull" );
		AlarmEntity alarmEn = new AlarmEntity();
		alarmEn.setProperty( "notNullAgain" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( en );
		s.persist( alarmEn );
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "id", new StandardAnalyzer() );
		{
			Query luceneQuery =  new MatchAllDocsQuery();
			FullTextQuery query = s.createFullTextQuery( luceneQuery );
			assertEquals( 2, query.getResultSize() );
		}
		{
			Query luceneQuery =  parser.parse( "entity:alarm" );
			FullTextQuery query = s.createFullTextQuery( luceneQuery, MyEntity.class );
			assertEquals( 1, query.getResultSize() );
		}
		{
			Query luceneQuery = parser.parse( "property:sound" );
			FullTextQuery query = s.createFullTextQuery( luceneQuery, AlarmEntity.class );
			assertEquals( 0, query.getResultSize() );
		}
		
		tx.commit();
		s.close();
	}

}
