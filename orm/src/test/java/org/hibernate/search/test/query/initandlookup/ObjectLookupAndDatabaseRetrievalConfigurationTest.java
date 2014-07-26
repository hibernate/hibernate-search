/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.initandlookup;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A bunch of indirect tests to verify that object lookup method and database retrieval method are configurable.
 * Assertions are via Byteman determining that the appropriate object initializer was used.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1119")
@RunWith(BMUnitRunner.class)
public class ObjectLookupAndDatabaseRetrievalConfigurationTest {

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.CriteriaObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testDefaultLookupMethod() throws Exception {
		try ( FullTextSessionBuilder builder = buildFullTextSessionBuilder( null, null ) ) {
			indexTestData( builder );
			FullTextSession fullTextSession = builder.openFullTextSession();
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
			query.list();
			fullTextSession.close();

			Assert.assertEquals(
					"CriteriaObjectInitializer should have been used as object initializer",
					1,
					BytemanHelper.getAndResetInvocationCount()
			);
		}
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.PersistenceContextObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetLookupMethodPersistenceContextUpperCase() throws Exception {
		try ( FullTextSessionBuilder builder = buildFullTextSessionBuilder( "PERSISTENCE_CONTEXT", null ) ) {
			indexTestData( builder );
			FullTextSession fullTextSession = builder.openFullTextSession();
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
			query.list();
			fullTextSession.close();

			Assert.assertEquals(
					"PersistenceContextObjectInitializer should have been used as object initializer",
					1,
					BytemanHelper.getAndResetInvocationCount()
			);
		}
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.PersistenceContextObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetLookupMethodPersistenceContextLowerCase() throws Exception {
		try ( FullTextSessionBuilder builder = buildFullTextSessionBuilder( "persistence_context", null ) ) {
			indexTestData( builder );
			FullTextSession fullTextSession = builder.openFullTextSession();
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
			query.list();
			fullTextSession.close();

			Assert.assertEquals(
					"PersistenceContextObjectInitializer should have been used as object initializer",
					1,
					BytemanHelper.getAndResetInvocationCount()
			);
		}
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.LookupObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetDatabaseRetrievalMethodUpperCase() throws Exception {
		try ( FullTextSessionBuilder builder = buildFullTextSessionBuilder( null, "FIND_BY_ID" ) ) {
			indexTestData( builder );
			FullTextSession fullTextSession = builder.openFullTextSession();
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
			query.list();
			fullTextSession.close();

			Assert.assertEquals(
					"LookupObjectInitializer should have been used as object initializer",
					1,
					BytemanHelper.getAndResetInvocationCount()
			);
		}
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.query.hibernate.impl.LookupObjectInitializer",
			targetMethod = "initializeObjects(org.hibernate.search.query.engine.spi.EntityInfo[], java.util.LinkedHashMap, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSetLookupMethodPersistenceContext")
	public void testSetDatabaseRetrievalMethodLoweCase() throws Exception {
		try ( FullTextSessionBuilder builder = buildFullTextSessionBuilder( null, "find_by_id" ) ) {
			indexTestData( builder );
			FullTextSession fullTextSession = builder.openFullTextSession();
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
			query.list();
			fullTextSession.close();

			Assert.assertEquals(
					"LookupObjectInitializer should have been used as object initializer",
					1,
					BytemanHelper.getAndResetInvocationCount()
			);
		}
	}

	@Test
	public void testSetInvalidLookupMethodThrowsException() throws Exception {
		try {
			try ( FullTextSessionBuilder builder = buildFullTextSessionBuilder( "foo", null ) ) {
				fail( "The setup of the search factory should have failed due to invalid value." );
			}
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000217" ) );
		}
	}

	@Test
	public void testSetInvalidRetrievalMethodThrowsException() throws Exception {
		try {
			try ( FullTextSessionBuilder builder = buildFullTextSessionBuilder( null, "foo" ) ) {
				fail( "The setup of the search factory should have failed due to invalid value." );
			}
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000217" ) );
		}
	}

	private FullTextSessionBuilder buildFullTextSessionBuilder(String objectLookUpMethod, String databaseRetrievalMethod) {
		FullTextSessionBuilder fullTextSessionBuilder = new FullTextSessionBuilder()
				.addAnnotatedClass( Foo.class );
		if ( objectLookUpMethod != null ) {
			fullTextSessionBuilder.setProperty( "hibernate.search.query.object_lookup_method", objectLookUpMethod );
		}
		if ( databaseRetrievalMethod != null ) {
			fullTextSessionBuilder.setProperty( "hibernate.search.query.database_retrieval_method", databaseRetrievalMethod );
		}
		return fullTextSessionBuilder.build();
	}

	private void indexTestData(FullTextSessionBuilder builder) {
		FullTextSession session = builder.openFullTextSession();
		Transaction transaction = session.beginTransaction();

		// need to index at least two instances. In the single instance case a search will not go via the object initializer
		Foo foo = new Foo();
		session.persist( foo );
		Foo snafu = new Foo();
		session.persist( snafu );

		transaction.commit();
		session.close();
	}

	@Indexed
	@Entity
	public static class Foo {
		@Id
		@GeneratedValue
		int id;
	}
}
