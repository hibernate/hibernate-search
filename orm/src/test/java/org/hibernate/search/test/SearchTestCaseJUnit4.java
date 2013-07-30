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

import org.hibernate.HibernateException;
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
	private DefaultTestResourceManager testResourceManager;

	@Before
	public void setUp() throws Exception {
		if ( testResourceManager == null || testResourceManager.needsConfigurationRebuild() ) {
			testResourceManager = new DefaultTestResourceManager( getAnnotatedClasses() );
			configure( testResourceManager.getCfg() );
			testResourceManager.buildConfiguration();
		}
		testResourceManager.openSessionFactory();
	}

	@After
	public void tearDown() throws Exception {
		testResourceManager.defaultTearDown();
	}

	@Override
	public final Configuration getCfg() {
		return testResourceManager.getCfg();
	}

	@Override
	public final void openSessionFactory() {
		testResourceManager.openSessionFactory();
	}

	@Override
	public final void closeSessionFactory() {
		testResourceManager.closeSessionFactory();
	}

	@Override
	public final SessionFactory getSessionFactory() {
		return testResourceManager.getSessionFactory();
	}

	@Override
	public final SearchFactory getSearchFactory() {
		return testResourceManager.getSearchFactory();
	}

	@Override
	public SearchFactoryImplementor getSearchFactoryImpl() {
		return testResourceManager.getSearchFactoryImpl();
	}

	@Override
	public final Session openSession() throws HibernateException {
		return testResourceManager.openSession();
	}

	@Override
	public final Session getSession() {
		return testResourceManager.getSession();
	}

	@Override
	public void ensureIndexesAreEmpty() {
		testResourceManager.ensureIndexesAreEmpty();
	}

	@Override
	public File getBaseIndexDir() {
		return testResourceManager.getBaseIndexDir();
	}

	@Override
	public Directory getDirectory(Class<?> clazz) {
		return testResourceManager.getDirectory( clazz );
	}

	@Override
	public void forceConfigurationRebuild() {
		if ( testResourceManager == null ) {
			testResourceManager = new DefaultTestResourceManager( getAnnotatedClasses() );
		}
		testResourceManager.forceConfigurationRebuild();
	}

	@Override
	public boolean needsConfigurationRebuild() {
		return testResourceManager.needsConfigurationRebuild();
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected void configure(Configuration cfg) {
		testResourceManager.applyDefaultConfiguration( cfg );
	}
}
