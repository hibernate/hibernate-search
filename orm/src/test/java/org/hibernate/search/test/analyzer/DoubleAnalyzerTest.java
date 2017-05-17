/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

/**
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-263")
@Category(SkipOnElasticsearch.class) // Custom analyzer implementations cannot be used with Elasticsearch.
public class DoubleAnalyzerTest extends SearchTestBase {

	public static final Log log = LoggerFactory.make();

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class, AlarmEntity.class };
	}

	@Test
	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setEntity( "anyNotNull" );
		AlarmEntity alarmEn = new AlarmEntity();
		alarmEn.setProperty( "notNullAgain" );
		alarmEn.setAlarmDescription( "description" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( en );
		s.persist( alarmEn );
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "id", TestConstants.keywordAnalyzer );
		{
			Query luceneQuery = new MatchAllDocsQuery();
			FullTextQuery query = s.createFullTextQuery( luceneQuery );
			assertEquals( 2, query.getResultSize() );
		}
		{
			Query luceneQuery = parser.parse( "entity:alarm" );
			FullTextQuery query = s.createFullTextQuery( luceneQuery, MyEntity.class );
			assertEquals( 1, query.getResultSize() );
		}
		{
			Query luceneQuery = parser.parse( "property:sound" );
			FullTextQuery query = s.createFullTextQuery( luceneQuery, AlarmEntity.class );
			assertEquals( 0, query.getResultSize() );
		}
		{
			Query luceneQuery = parser.parse( "description_analyzer2:sound" );
			FullTextQuery query = s.createFullTextQuery( luceneQuery, AlarmEntity.class );
			assertEquals( 1, query.getResultSize() );
		}
		{
			Query luceneQuery = parser.parse( "description_analyzer3:music" );
			FullTextQuery query = s.createFullTextQuery( luceneQuery, AlarmEntity.class );
			assertEquals( 1, query.getResultSize() );
		}
		{
			Query luceneQuery = parser.parse( "description_normalizer1:symphony" );
			FullTextQuery query = s.createFullTextQuery( luceneQuery, AlarmEntity.class );
			assertEquals( 1, query.getResultSize() );
		}

		tx.commit();
		s.close();
	}
}
