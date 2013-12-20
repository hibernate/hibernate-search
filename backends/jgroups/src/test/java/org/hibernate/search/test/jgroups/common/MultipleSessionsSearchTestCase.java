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
package org.hibernate.search.test.jgroups.common;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.test.SearchTestCaseJUnit4;

/**
 * Test class to simulate clustered environment (one master, and one slave node)
 *
 * @author Lukasz Moren
 */
public abstract class MultipleSessionsSearchTestCase extends SearchTestCaseJUnit4 {

	protected static final String masterCopy = "/master/copy";

	/**
	 * The lucene index directory which is specific to the master node.
	 */
	protected static final String masterMain = "/master/main";

	/**
	 * The lucene index directory which is specific to the slave node.
	 */
	protected static final String slave = "/slave";


	protected static SessionFactory slaveSessionFactory;

	/**
	 * Common configuration for all slave nodes
	 */
	private Configuration commonCfg;

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );

		//master
		cfg.setProperty( "hibernate.search.default.sourceBase", getBaseIndexDir().getAbsolutePath() + masterCopy );
		cfg.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() + masterMain );
		cfg.setProperty( "hibernate.search.default.refresh", "1" );
		cfg.setProperty(
				"hibernate.search.default.directory_provider", "filesystem-master"
		);
	}

	protected void commonConfigure(Configuration cfg) {
		super.configure( cfg );

		//slave(s)
		cfg.setProperty( "hibernate.search.default.sourceBase", getBaseIndexDir().getAbsolutePath() + masterCopy );
		cfg.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() + slave );
		cfg.setProperty( "hibernate.search.default.refresh", "1" );
		cfg.setProperty(
				"hibernate.search.default.directory_provider", "filesystem-slave"
		);
		cfg.setProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		buildSlaveSessionFactory();
	}

	@Override
	public void tearDown() throws Exception {
		//close session factories
		if ( slaveSessionFactory != null ) {
			slaveSessionFactory.close();
			slaveSessionFactory = null;
		}
		super.tearDown();
	}

	private void buildSlaveSessionFactory() throws Exception {
		if ( slaveSessionFactory != null ) {
			throw new IllegalStateException( "slaveSessionFactory already created" );
		}
		setCommonCfg( new Configuration() );
		commonConfigure( commonCfg );
		for ( Class<?> aClass : getCommonAnnotatedClasses() ) {
			getCommonConfiguration().addAnnotatedClass( aClass );
		}
		slaveSessionFactory = getCommonConfiguration().buildSessionFactory();
	}

	private void setCommonCfg(Configuration configuration) {
		this.commonCfg = configuration;
	}

	protected Configuration getCommonConfiguration() {
		return commonCfg;
	}

	protected Session getSlaveSession() {
		return slaveSessionFactory.openSession();
	}

	protected static SessionFactory getSlaveSessionFactory() {
		return slaveSessionFactory;
	}

	@Override
	protected abstract Class<?>[] getAnnotatedClasses();

	protected abstract Class<?>[] getCommonAnnotatedClasses();
}
