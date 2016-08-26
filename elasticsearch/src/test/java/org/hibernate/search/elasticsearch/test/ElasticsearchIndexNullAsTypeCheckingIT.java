/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.DefaultTestResourceManager;
import org.hibernate.search.test.util.TestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Unit tests for {@link ElasticsearchIndexManager}'s indexNullAs type checking.
 *
 * @author Yoann Rodiere
 */
@RunWith(Suite.class)
@SuiteClasses(value = {
		ElasticsearchIndexNullAsTypeCheckingIT.BooleanFailureTest.class,
		ElasticsearchIndexNullAsTypeCheckingIT.DateFailureTest.class
})
public class ElasticsearchIndexNullAsTypeCheckingIT {

	protected abstract static class AbstractIndexNullAsFailureTest implements TestConfiguration {
		@Rule
		public ExpectedException thrown = ExpectedException.none();

		private DefaultTestResourceManager testResourceManager;

		protected void init() {
			getTestResourceManager().openSessionFactory();
		}

		// synchronized due to lazy initialization (source: SearchTestBase.java)
		private synchronized DefaultTestResourceManager getTestResourceManager() {
			if ( testResourceManager == null ) {
				testResourceManager = new DefaultTestResourceManager( this, this.getClass() );
			}
			return testResourceManager;
		}

		@Override
		public void configure(Map<String, Object> settings) {
			settings.put(
					ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
					IndexSchemaManagementStrategy.CREATE
					);
		}

		@Override
		public Set<String> multiTenantIds() {
			// Empty by default; specify more than one tenant to enable multi-tenancy
			return Collections.emptySet();
		}
	}

	public static class BooleanFailureTest extends AbstractIndexNullAsFailureTest {
		@Test
		public void indexNullAs_invalid_boolean() {
			thrown.expect( SearchException.class );
			thrown.expectMessage( "HSEARCH400027" );
			thrown.expectMessage( "Boolean" );
			thrown.expectMessage( "myField" );

			init();
		}

		@Override
		public Class<?>[] getAnnotatedClasses() {
			return new Class[] { BooleanFailureTestEntity.class };
		}

		@Indexed
		@Entity
		public static class BooleanFailureTestEntity {
			@DocumentId
			@Id
			Long id;

			@Field(indexNullAs = "foo")
			boolean myField;
		}
	}

	public static class DateFailureTest extends AbstractIndexNullAsFailureTest {
		@Test
		public void indexNullAs_invalid_boolean() {
			thrown.expect( SearchException.class );
			thrown.expectMessage( "HSEARCH400028" );
			thrown.expectMessage( "Date" );
			thrown.expectMessage( "myField" );

			init();
		}

		@Override
		public Class<?>[] getAnnotatedClasses() {
			return new Class[] { DateFailureTestEntity.class };
		}

		@Indexed
		@Entity
		public static class DateFailureTestEntity {
			@DocumentId
			@Id
			Long id;

			@Field(indexNullAs = "01/01/2013") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ssZ)
			Date myField;
		}
	}

}
