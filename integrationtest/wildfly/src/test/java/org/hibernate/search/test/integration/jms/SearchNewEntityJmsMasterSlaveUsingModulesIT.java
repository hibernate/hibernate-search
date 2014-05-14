/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms;

import java.io.File;

import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;
import org.hibernate.search.test.integration.wildfly.ModuleMemberRegistrationIT;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * Execute the tests in {@link SearchNewEntityJmsMasterSlave} using the modules in JBoss AS to add the required
 * dependencies.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
@Ignore("Need to figure out how to configure WildFly: HSEARCH-1440")
public class SearchNewEntityJmsMasterSlaveUsingModulesIT extends SearchNewEntityJmsMasterSlave {

	private static final File tmpDir = RegistrationConfiguration.createTempDir();

	@Deployment(name = "master", order = 1)
	public static Archive<?> createDeploymentMaster() throws Exception {
		Archive<?> master = DeploymentJmsMasterSlave.createMaster( "master", REFRESH_PERIOD_IN_SEC, tmpDir );
		addDependecies( master );
		return master;
	}

	@Deployment(name = "slave-1", order = 2)
	public static Archive<?> createDeploymentSlave1() throws Exception {
		Archive<?> slave = DeploymentJmsMasterSlave.createSlave( "slave-1", REFRESH_PERIOD_IN_SEC, tmpDir );
		addDependecies( slave );
		return slave;
	}

	@Deployment(name = "slave-2", order = 3)
	public static Archive<?> createDeploymentSlave2() throws Exception {
		Archive<?> slave = DeploymentJmsMasterSlave.createSlave( "slave-2", REFRESH_PERIOD_IN_SEC, tmpDir );
		addDependecies( slave );
		return slave;
	}

	private static void addDependecies(Archive<?> archive) {
		archive.add( ModuleMemberRegistrationIT.manifest(), "META-INF/MANIFEST.MF" );
	}

}
