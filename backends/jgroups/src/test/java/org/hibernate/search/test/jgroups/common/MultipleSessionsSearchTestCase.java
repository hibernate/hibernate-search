/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.common;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.test.DefaultTestResourceManager;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.search.test.util.TestConfiguration;
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

	private DefaultTestResourceManager slaveResources;

	@Override
	public void configure(Map<String,Object> cfg) {
		//master
		cfg.put(
				"hibernate.search.default.sourceBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + masterCopy
		);
		cfg.put(
				"hibernate.search.default.indexBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + masterMain
		);
		cfg.put( "hibernate.search.default.refresh", "1" );
		cfg.put(
				"hibernate.search.default.directory_provider", "filesystem-master"
		);
	}

	protected void configureSlave(Map<String,Object> cfg) {
		//slave(s)
		cfg.put(
				"hibernate.search.default.sourceBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + masterCopy
		);
		cfg.put(
				"hibernate.search.default.indexBase",
				TestConstants.getIndexDirectory( MultipleSessionsSearchTestCase.class ) + slave
		);
		cfg.put( "hibernate.search.default.refresh", "1" );
		cfg.put(
				"hibernate.search.default.directory_provider", "filesystem-slave"
		);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		buildSlaveSessionFactory();
	}

	@Override
	public void tearDown() throws Exception {
		//close session factories
		if ( slaveResources != null ) {
			slaveResources.closeSessionFactory();
			slaveResources = null;
		}
		super.tearDown();
	}

	private void buildSlaveSessionFactory() throws Exception {
		if ( slaveResources != null ) {
			throw new IllegalStateException( "Slave SessionFactory already created" );
		}
		HashMap<String, Object> slaveConfiguration = new HashMap<String,Object>();
		configureSlave( slaveConfiguration );
		TestConfiguration slaveTestConfiguration = new ImmutableTestConfiguration( slaveConfiguration, getAnnotatedClasses() );
		slaveResources = new DefaultTestResourceManager( slaveTestConfiguration, this.getClass() );
		slaveResources.openSessionFactory();
	}

	protected Session getSlaveSession() {
		return slaveResources.openSession();
	}

	protected SessionFactory getSlaveSessionFactory() {
		return slaveResources.getSessionFactory();
	}

}
