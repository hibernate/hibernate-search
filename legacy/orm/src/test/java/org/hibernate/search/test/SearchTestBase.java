/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.util.BackendTestHelper;
import org.hibernate.search.test.util.TestConfiguration;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.testing.junit4.CustomRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Base class for Hibernate Search tests using Hibernate ORM and Junit 4.
 *
 * @author Hardy Ferentschik
 */
@RunWith(CustomRunner.class)
public abstract class SearchTestBase implements TestResourceManager, TestConfiguration {

	protected static final Boolean PERFORMANCE_TESTS_ENABLED = TestConstants.arePerformanceTestsEnabled();

	// access only via getter, since instance gets lazily initialized
	private DefaultTestResourceManager testResourceManager;

	// access only via getter, since instance gets lazily initialized
	private BackendTestHelper backendTestHelper;

	@Before
	public void setUp() throws Exception {
		getTestResourceManager().openSessionFactory();
	}

	@After
	public void tearDown() throws Exception {
		getTestResourceManager().defaultTearDown();
	}

	@Override
	public final void openSessionFactory() {
		getTestResourceManager().openSessionFactory();
	}

	@Override
	public final void closeSessionFactory() {
		getTestResourceManager().closeSessionFactory();
	}

	@Override
	public final SessionFactory getSessionFactory() {
		return getTestResourceManager().getSessionFactory();
	}

	@Override
	public final SearchFactory getSearchFactory() {
		return getTestResourceManager().getSearchFactory();
	}

	@Override
	public ExtendedSearchIntegrator getExtendedSearchIntegrator() {
		return getTestResourceManager().getExtendedSearchIntegrator();
	}

	@Override
	public final Session openSession() {
		return getTestResourceManager().openSession();
	}

	@Override
	public final Session getSession() {
		return getTestResourceManager().getSession();
	}

	@Override
	public void ensureIndexesAreEmpty() throws IOException {
		getTestResourceManager().ensureIndexesAreEmpty();
	}

	@Override
	public Path getBaseIndexDir() {
		return getTestResourceManager().getBaseIndexDir();
	}

	@Override
	public void configure(Map<String,Object> settings) {
		//Empty by default
	}

	@Override
	public Set<String> multiTenantIds() {
		//Empty by default; specify more than one tenant to enable multi-tenancy
		return Collections.emptySet();
	}

	/**
	 * Use {@link #getNumberOfDocumentsInIndex(IndexedTypeIdentifier)}
	 * @param entityType
	 * @return
	 */
	@Deprecated
	protected int getNumberOfDocumentsInIndex(Class<?> entityType) {
		return getNumberOfDocumentsInIndex( new PojoIndexedTypeIdentifier( entityType ) );
	}

	protected int getNumberOfDocumentsInIndex(IndexedTypeIdentifier entityType) {
		return getBackendTestHelper().getNumberOfDocumentsInIndex( entityType );
	}

	protected int getNumberOfDocumentsInIndex(String indexName) {
		return getBackendTestHelper().getNumberOfDocumentsInIndex( indexName );
	}

	protected int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		return getBackendTestHelper().getNumberOfDocumentsInIndexByQuery(
				indexName, fieldName, value
		);
	}

	// synchronized due to lazy initialization
	private synchronized DefaultTestResourceManager getTestResourceManager() {
		if ( testResourceManager == null ) {
			testResourceManager = new DefaultTestResourceManager( this, this.getClass() );
		}
		return testResourceManager;
	}

	private BackendTestHelper getBackendTestHelper() {
		if ( backendTestHelper == null ) {
			backendTestHelper = BackendTestHelper.getInstance( getTestResourceManager() );
		}

		return backendTestHelper;
	}
}
