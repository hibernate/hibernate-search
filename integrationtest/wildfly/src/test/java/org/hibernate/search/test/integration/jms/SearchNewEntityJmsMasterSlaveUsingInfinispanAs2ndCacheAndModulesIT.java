/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms;

import static org.hibernate.search.test.integration.VersionTestHelper.addDependencyToSearchModule;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.hibernate.search.test.integration.jms.controller.StatisticsController;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the the combination of JMS+Infinspan as backend and the use of Infinispan as second level cache.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class SearchNewEntityJmsMasterSlaveUsingInfinispanAs2ndCacheAndModulesIT extends SearchNewEntityJmsMasterSlave {

	@Inject
	StatisticsController stats;

	@Test
	@InSequence(1005)
	@OperateOnDeployment("slave-1")
	public void secondLevelCacheShouldBeActive() throws Exception {
		RegisteredMember cachedMember = memberRegistration.getNewMember();
		cachedMember.setName( "Johnny Cached" );
		memberRegistration.register();

		// Cache the result
		memberRegistration.findById( cachedMember.getId() );
		memberRegistration.findById( cachedMember.getId() );
		memberRegistration.findById( cachedMember.getId() );

		long secondLevelCacheMissCount = stats.getStatistics().getSecondLevelCacheMissCount();
		assertTrue( "Second level cache not enabled", secondLevelCacheMissCount > 0 );
	}

	@Deployment(name = "master", order = 1)
	public static Archive<?> createDeploymentMaster() throws Exception {
		Archive<?> master = DeploymentJmsMasterSlaveAndInfinispanAs2ndLevelCache.createMaster( "master" );
		addDependencies( master );
		return master;
	}

	@Deployment(name = "slave-1", order = 2)
	public static Archive<?> createDeploymentSlave1() throws Exception {
		Archive<?> slave = DeploymentJmsMasterSlaveAndInfinispanAs2ndLevelCache.createSlave( "slave-1" );
		addDependencies( slave );
		return slave;
	}

	@Deployment(name = "slave-2", order = 3)
	public static Archive<?> createDeploymentSlave2() throws Exception {
		Archive<?> slave = DeploymentJmsMasterSlaveAndInfinispanAs2ndLevelCache.createSlave( "slave-2" );
		addDependencies( slave );
		return slave;
	}

	private static void addDependencies(Archive<?> archive) {
		addDependencyToSearchModule( archive );
	}
}
