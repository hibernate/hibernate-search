/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms.infinispan;

import static org.hibernate.search.test.integration.VersionTestHelper.addDependencyToSearchModule;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;

/**
 * Execute the tests in {@link SearchNewEntityJmsMasterSlaveAndInfinispan} using the modules in JBoss AS to add the required
 * dependencies.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class SearchNewEntityJmsMasterSlaveUsingInfinispanAndModulesIT extends SearchNewEntityJmsMasterSlaveAndInfinispan {

	@Deployment(name = "master", order = 1)
	public static Archive<?> createDeploymentMaster() throws Exception {
		Archive<?> master = DeploymentJmsMasterSlaveAndInfinispan.createMaster( "master" );
		addDependencies( master );
		return master;
	}

	@Deployment(name = "slave-1", order = 2)
	public static Archive<?> createDeploymentSlave1() throws Exception {
		Archive<?> slave = DeploymentJmsMasterSlaveAndInfinispan.createSlave( "slave-1" );
		addDependencies( slave );
		return slave;
	}

	@Deployment(name = "slave-2", order = 3)
	public static Archive<?> createDeploymentSlave2() throws Exception {
		Archive<?> slave = DeploymentJmsMasterSlaveAndInfinispan.createSlave( "slave-2" );
		addDependencies( slave );
		return slave;
	}

	private static void addDependencies(Archive<?> archive) {
		addDependencyToSearchModule( archive, "org.infinispan" );
	}
}
