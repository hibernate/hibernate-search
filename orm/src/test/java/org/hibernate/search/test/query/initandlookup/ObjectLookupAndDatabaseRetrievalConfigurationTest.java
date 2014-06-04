/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.initandlookup;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A bunch of indirect tests to verify that object lookup method and database retrieval method are configurable.
 * Assertions are via Byteman determining that the appropriate object initializer was used.
 *
 * author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1119")
@RunWith(BMUnitRunner.class)
public class ObjectLookupAndDatabaseRetrievalConfigurationTest extends SearchTestBase {
	private String objectLookUpMethod;
	private String databaseRetrievalMethod;

	@After
	public void tearDown() {
		objectLookUpMethod = null;
		databaseRetrievalMethod = null;
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.CriteriaObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testDefaultLookupMethod() throws Exception {
		// need to re-setup in the actual test method in order to bootstrap with different configuration setting
		closeSessionFactory();
		forceConfigurationRebuild();
		setUp();
		indexTestData();

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		query.list();

		Assert.assertEquals(
				"CriteriaObjectInitializer should have been used as object initializer",
				1,
				BytemanHelper.getAndResetInvocationCount()
		);
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.PersistenceContextObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetLookupMethodPersistenceContextUpperCase() throws Exception {
		// need to re-setup in the actual test method in order to bootstrap with different configuration setting
		closeSessionFactory();
		forceConfigurationRebuild();
		objectLookUpMethod = "PERSISTENCE_CONTEXT";
		setUp();
		indexTestData();

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		query.list();

		Assert.assertEquals(
				"PersistenceContextObjectInitializer should have been used as object initializer",
				1,
				BytemanHelper.getAndResetInvocationCount()
		);
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.PersistenceContextObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetLookupMethodPersistenceContextLowerCase() throws Exception {
		// need to re-setup in the actual test method in order to bootstrap with different configuration setting
		closeSessionFactory();
		forceConfigurationRebuild();
		objectLookUpMethod = "persistence_context";
		setUp();
		indexTestData();

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		query.list();

		Assert.assertEquals(
				"PersistenceContextObjectInitializer should have been used as object initializer",
				1,
				BytemanHelper.getAndResetInvocationCount()
		);
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.LookupObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetDatabaseRetrievalMethodUpperCase() throws Exception {
		// need to re-setup in the actual test method in order to bootstrap with different configuration setting
		closeSessionFactory();
		forceConfigurationRebuild();
		databaseRetrievalMethod = "FIND_BY_ID";
		setUp();
		indexTestData();

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		query.list();

		Assert.assertEquals(
				"LookupObjectInitializer should have been used as object initializer",
				1,
				BytemanHelper.getAndResetInvocationCount()
		);
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.LookupObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetDatabaseRetrievalMethodLoweCase() throws Exception {
		// need to re-setup in the actual test method in order to bootstrap with different configuration setting
		closeSessionFactory();
		forceConfigurationRebuild();
		databaseRetrievalMethod = "find_by_id";
		setUp();
		indexTestData();

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		query.list();

		Assert.assertEquals(
				"LookupObjectInitializer should have been used as object initializer",
				1,
				BytemanHelper.getAndResetInvocationCount()
		);
	}


	@Test
	public void testSetInvalidLookupMethodThrowsException() throws Exception {
		closeSessionFactory();
		forceConfigurationRebuild();
		objectLookUpMethod = "foo";
		try {
			setUp();
			fail( "The setup of the search factory should have failed due to invalid value." );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000217" ) );
		}
	}

	@Test
	public void testSetInvalidRetrievalMethodThrowsException() throws Exception {
		closeSessionFactory();
		forceConfigurationRebuild();
		databaseRetrievalMethod = "foo";
		try {
			setUp();
			fail( "The setup of the search factory should have failed due to invalid value." );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000217" ) );
		}
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		if ( objectLookUpMethod != null ) {
			cfg.setProperty( "hibernate.search.query.object_lookup_method", objectLookUpMethod );
		}
		if ( databaseRetrievalMethod != null ) {
			cfg.setProperty( "hibernate.search.query.database_retrieval_method", databaseRetrievalMethod );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	private void indexTestData() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// need to index at least two instances. In the single instance case a search will not go via the object initializer
		Foo foo = new Foo();
		session.persist( foo );
		Foo snafu = new Foo();
		session.persist( snafu );

		transaction.commit();
	}

	@Indexed
	@Entity
	public static class Foo {
		@Id
		@GeneratedValue
		int id;
	}
}
