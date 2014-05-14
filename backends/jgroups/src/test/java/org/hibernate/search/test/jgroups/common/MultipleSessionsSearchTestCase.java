/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.common;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

/**
 * Test class to simulate clustered environment (one master, and one slave node)
 *
 * @author Lukasz Moren
 */
public abstract class MultipleSessionsSearchTestCase extends SearchTestBase {

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
		cfg.setProperty(
				"hibernate.search.default.sourceBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + masterCopy
		);
		cfg.setProperty(
				"hibernate.search.default.indexBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + masterMain
		);
		cfg.setProperty( "hibernate.search.default.refresh", "1" );
		cfg.setProperty(
				"hibernate.search.default.directory_provider", "filesystem-master"
		);
	}

	protected void commonConfigure(Configuration cfg) {
		super.configure( cfg );

		//slave(s)
		cfg.setProperty(
				"hibernate.search.default.sourceBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + masterCopy
		);
		cfg.setProperty(
				"hibernate.search.default.indexBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + slave
		);
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
