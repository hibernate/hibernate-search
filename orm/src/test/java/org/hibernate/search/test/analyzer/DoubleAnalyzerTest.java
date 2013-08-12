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
package org.hibernate.search.test.analyzer;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.TestForIssue;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-263")
public class DoubleAnalyzerTest extends SearchTestCase {

	public static final Log log = LoggerFactory.make();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class, AlarmEntity.class };
	}

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
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"id",
				TestConstants.standardAnalyzer
		);
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

		tx.commit();
		s.close();
	}
}
