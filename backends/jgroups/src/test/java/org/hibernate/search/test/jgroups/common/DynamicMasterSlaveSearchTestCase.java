/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.DefaultTestResourceManager;
import org.hibernate.search.test.util.TestConfiguration;
import org.junit.After;
import org.junit.Before;

/**
 * Test class to simulate clustered environment with one master and one or more slave nodes,
 * each role being determined dynamically.
 *
 * @author Lukasz Moren
 */
public abstract class DynamicMasterSlaveSearchTestCase implements TestConfiguration {

	private String alreadySelectedBaseIndexDir = null;
	private List<DefaultTestResourceManager> resourceManagers = new ArrayList<>();

	@Override
	public void configure(Map<String,Object> cfg) {
		/*
		 * Configure all nodes to read/write to the exact same index on disk.
		 * This will lead to bad performance, but it's also the only way to
		 * use dynamic master selection without an infinispan directory provider.
		 */
		cfg.put( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroups" );
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.put( "hibernate.search.default.exclusive_index_use", "false" );
		if ( alreadySelectedBaseIndexDir != null ) {
			cfg.put( "hibernate.search.default.indexBase", alreadySelectedBaseIndexDir );
		}
	}

	@Override
	public Set<String> multiTenantIds() {
		return Collections.emptySet();
	}

	protected abstract int getExpectedNumberOfNodes();

	@Before
	public void setUp() throws Exception {
		for ( int i = 0 ; i < getExpectedNumberOfNodes() ; ++i ) {
			DefaultTestResourceManager resourceManager = new DefaultTestResourceManager( this, getClass() );
			resourceManagers.add( resourceManager );
			resourceManager.openSessionFactory();
			if ( alreadySelectedBaseIndexDir == null ) {
				// Set the base index dir to the dir selected for the first search factory
				this.alreadySelectedBaseIndexDir = resourceManager.getBaseIndexDir().toAbsolutePath().toString();
			}
		}
	}

	@After
	public void tearDown() throws Exception {
		for ( DefaultTestResourceManager resourceManager : resourceManagers ) {
			resourceManager.close();
		}
		/*
		 * Only do this after closing every resource manager,
		 * so as not to delete files that are still being used.
		 */
		for ( DefaultTestResourceManager resourceManager : resourceManagers ) {
			resourceManager.cleanUp();
		}
	}

	public List<DefaultTestResourceManager> getResourceManagers() {
		return resourceManagers;
	}

}
