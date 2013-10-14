/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.integration.jms;

import java.io.File;

import org.hibernate.search.Version;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * Execute the tests in {@link SearchNewEntityJmsMasterSlave} adding the the dependencies as jars in the
 * deployments.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
@Ignore("Need to figure out how to configure WildFly: HSEARCH-1440")
public class SearchNewEntityJmsMasterSlaveIT extends SearchNewEntityJmsMasterSlave {

	private static final File tmpDir = RegistrationConfiguration.createTempDir();

	/**
	 * Lazy initialization of the libraries since Maven is painfully slow.
	 *
	 * @author Davide D'Alto <davide@hibernate.org>
	 */
	private static class LibrariesLoader {
		public static final File[] LIBRARIES = init();

		private static File[] init() {
			MavenDependencyResolver resolver = DependencyResolvers.use( MavenDependencyResolver.class );
			String currentVersion = Version.getVersionString();
			File[] libraryFiles = resolver
					.artifact( "org.hibernate:hibernate-search-orm:" + currentVersion )
					.exclusion( "org.hibernate:hibernate-entitymanager" )
					.exclusion( "org.hibernate:hibernate-core" )
					.exclusion( "org.hibernate:hibernate-search-analyzers" )
					.exclusion( "org.jboss.logging:jboss-logging" )
					.exclusion( "org.slf4j:slf4j-api" )
					.resolveAsFiles();
			return libraryFiles;
		}
	}

	@Deployment(name = "master", order = 1)
	public static Archive<?> createDeploymentMaster() throws Exception {
		WebArchive master = DeploymentJmsMasterSlave.createMaster( "master", REFRESH_PERIOD_IN_SEC, tmpDir )
				.as( WebArchive.class );
		addLibraries( master );
		return master;
	}

	@Deployment(name = "slave-1", order = 2)
	public static Archive<?> createDeploymentSlave1() throws Exception {
		WebArchive slave = DeploymentJmsMasterSlave.createSlave( "slave-1", REFRESH_PERIOD_IN_SEC, tmpDir )
				.as( WebArchive.class );
		addLibraries( slave );
		return slave;
	}

	@Deployment(name = "slave-2", order = 3)
	public static Archive<?> createDeploymentSlave2() throws Exception {
		WebArchive slave = DeploymentJmsMasterSlave.createSlave( "slave-2", REFRESH_PERIOD_IN_SEC, tmpDir )
				.as( WebArchive.class );
		addLibraries( slave );
		return slave;
	}

	private static void addLibraries(WebArchive archive) {
		archive.addAsLibraries( LibrariesLoader.LIBRARIES );
	}
}
