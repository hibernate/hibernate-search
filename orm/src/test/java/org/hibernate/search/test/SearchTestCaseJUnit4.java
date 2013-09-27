/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test;

import java.io.File;

import org.apache.lucene.store.Directory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for Hibernate Search tests using Hibernate ORM and Junit 4.
 *
 * @author Hardy Ferentschik
 */
public abstract class SearchTestCaseJUnit4 implements TestResourceManager {
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
	public SearchFactoryImplementor getSearchFactoryImpl() {
		return getTestResourceManager().getSearchFactoryImpl();
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
