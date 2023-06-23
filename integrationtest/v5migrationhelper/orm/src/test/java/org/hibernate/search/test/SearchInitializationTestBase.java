/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.test.testsupport.V5MigrationHelperOrmSetupHelper;
import org.hibernate.search.test.util.BackendTestHelper;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.search.test.util.TestConfiguration;

import org.junit.After;
import org.junit.Rule;

/**
 * A base class for tests of the initialization process.
 *
 * <p>The main difference with {@link SearchTestBase} is that the test itself does not implement
 * {@link TestConfiguration}, which enables using different configurations for different test methods.
 * <p>When subclassing this class, Hibernate Search initialization must be triggered manually by calling
 * {@link #init(TestConfiguration)}.
 * <p>This is most commonly used to assert exceptions are thrown in specific cases.
 *
 * @author Yoann Rodiere
 */
public abstract class SearchInitializationTestBase {

	@Rule
	public final V5MigrationHelperOrmSetupHelper setupHelper = V5MigrationHelperOrmSetupHelper.create();

	private DefaultTestResourceManager testResourceManager;

	private BackendTestHelper backendTestHelper;

	/**
	 * @param configuration The test configuration to use when initializing.
	 * @see ImmutableTestConfiguration
	 */
	protected void init(TestConfiguration configuration) {
		if ( testResourceManager == null ) {
			testResourceManager = new DefaultTestResourceManager( configuration, setupHelper );
		}
		testResourceManager.openSessionFactory();
	}

	/**
	 * Initialize with no particular settings
	 * @param annotatedClasses The classes to search for annotations
	 */
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	/**
	 * @return the testResourceManager, or null if there isn't any (i.e. if {@link #init(TestConfiguration)}
	 * hasn't been called yet or if {@link #tearDown()} has been called)
	 */
	protected TestResourceManager getTestResourceManager() {
		return testResourceManager;
	}

	protected BackendTestHelper getBackendTestHelper() {
		if ( backendTestHelper == null ) {
			backendTestHelper = BackendTestHelper.getInstance( getTestResourceManager() );
		}

		return backendTestHelper;
	}

	@After
	public void tearDown() throws Exception {
		try {
			if ( testResourceManager != null ) {
				testResourceManager.defaultTearDown();
			}
		}
		finally {
			testResourceManager = null;
		}
	}
}
