/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test;

import java.io.File;

import org.apache.lucene.store.Directory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
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
public abstract class SearchTestBase implements TestResourceManager {

	protected static final Boolean PERFORMANCE_TESTS_ENABLED = TestConstants.arePerformanceTestsEnabled();

	// access only via getter, since instance gets lazily initalized
	private DefaultTestResourceManager testResourceManager;

	@Before
	public void setUp() throws Exception {
		DefaultTestResourceManager testResourceManager = getTestResourceManager();
		if ( testResourceManager.needsConfigurationRebuild() ) {
			configure( testResourceManager.getCfg() );
			testResourceManager.buildConfiguration();
		}
		testResourceManager.openSessionFactory();
	}

	@After
	public void tearDown() throws Exception {
		getTestResourceManager().defaultTearDown();
	}

	@Override
	public final Configuration getCfg() {
		return getTestResourceManager().getCfg();
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
	public void ensureIndexesAreEmpty() {
		getTestResourceManager().ensureIndexesAreEmpty();
	}

	@Override
	public File getBaseIndexDir() {
		return getTestResourceManager().getBaseIndexDir();
	}

	@Override
	public Directory getDirectory(Class<?> clazz) {
		return getTestResourceManager().getDirectory( clazz );
	}

	@Override
	public void forceConfigurationRebuild() {
		getTestResourceManager().forceConfigurationRebuild();
	}

	@Override
	public boolean needsConfigurationRebuild() {
		return getTestResourceManager().needsConfigurationRebuild();
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected void configure(Configuration cfg) {
		getTestResourceManager().applyDefaultConfiguration( cfg );
	}

	// synchronized due to lazy initialization
	private synchronized DefaultTestResourceManager getTestResourceManager() {
		if ( testResourceManager == null ) {
			testResourceManager = new DefaultTestResourceManager( getAnnotatedClasses() );
		}
		return testResourceManager;
	}
}
