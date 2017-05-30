/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.objectloading;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.BytemanHelper.BytemanAccessor;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Tests for effectiveness of result loading.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-704")
@RunWith(BMUnitRunner.class)
public class ObjectLoaderHelperTest extends SearchTestBase {

	@Rule
	public BytemanAccessor byteman = BytemanHelper.createAccessor();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		for ( int i = 0; i < 100; i++ ) {
			TestEntity entity = new TestEntity( i, "document-" + i );
			fullTextSession.persist( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	@Test
	@BMRule(targetClass = "org.hibernate.internal.CriteriaImpl",
			targetMethod = "<init>(String, String, org.hibernate.engine.spi.SharedSessionContractImplementor)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testOnlyOneCriteriaQueryIsUsedToLoadMatchedEntities")
	public void testOnlyOneCriteriaQueryIsUsedToLoadMatchedEntities() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		Query luceneQuery = new MatchAllDocsQuery();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, TestEntity.class );
		int hitCount = fullTextQuery.list().size();
		assertEquals( "Wrong hit count", 100, hitCount );

		tx.commit();
		fullTextSession.close();

		Assert.assertEquals(
				"There should be only a single criteria query (CriteriaObjectsInitializer)",
				1,
				byteman.getAndResetInvocationCount()
		);
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}
}
