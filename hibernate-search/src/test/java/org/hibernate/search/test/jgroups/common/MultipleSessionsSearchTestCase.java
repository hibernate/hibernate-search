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

import java.io.InputStream;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.FileHelper;

/**
 * Test class to simulate clustered environment (one master, and one slave node)
 *
 * @author Lukasz Moren
 */
public abstract class MultipleSessionsSearchTestCase extends SearchTestCase {

	private static final String masterCopy = "/master/copy";

	/**
	 * The lucene index directory which is specific to the master node.
	 */
	private static final String masterMain = "/master/main";

	/**
	 * The lucene index directory which is specific to the slave node.
	 */
	private static final String slave = "/slave";


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
		//keep the fqcn to make sure they still work after the introduction of shortcuts
		cfg.setProperty(
				"hibernate.search.default.directory_provider", "org.hibernate.search.store.FSMasterDirectoryProvider"
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
	}

	@Override
	protected void setUp() throws Exception {
		if ( getBaseIndexDir().exists() ) {
			FileHelper.delete( getBaseIndexDir() );
		}
		super.setUp();
		buildCommonSessionFactory();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		//close session factories and clean index files
		if ( slaveSessionFactory != null ) {
			slaveSessionFactory.close();
		}
		FileHelper.delete( getBaseIndexDir() );
	}

	private void buildCommonSessionFactory() throws Exception {
		if ( getSlaveSessionFactory() != null ) {
			getSlaveSessionFactory().close();
		}

		setCommonCfg( new Configuration() );
		commonConfigure( commonCfg );
		if ( recreateSchema() ) {
			commonCfg.setProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );
		}
		for ( String aPackage : getCommonAnnotatedPackages() ) {
			( ( Configuration ) getCommonConfiguration() ).addPackage( aPackage );
		}
		for ( Class<?> aClass : getCommonAnnotatedClasses() ) {
			( ( Configuration ) getCommonConfiguration() ).addAnnotatedClass( aClass );
		}
		for ( String xmlFile : getCommonXmlFiles() ) {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
			getCommonConfiguration().addInputStream( is );
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

	private String[] getCommonAnnotatedPackages() {
		return new String[] { };
	}

	private String[] getCommonXmlFiles() {
		return new String[] { };
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected abstract Class<?>[] getCommonAnnotatedClasses();
}
