/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.Version;
import org.hibernate.search.test.integration.VersionTestHelper;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.runner.RunWith;

/**
 * Execute the tests in {@link SearchNewEntityJmsMasterSlave} adding the the dependencies as jars in the
 * deployments.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class SearchNewEntityJmsMasterSlaveIT extends SearchNewEntityJmsMasterSlave {

	private static final File tmpDir = RegistrationConfiguration.createTempDir();

	/**
	 * Lazy initialization of the libraries since Maven is painfully slow.
	 */
	private static class LibrariesLoader {
		public static final File[] LIBRARIES = init();

		private static File[] init() {
			final String currentVersion = Version.getVersionString();
			Set<File> libraryFiles = new HashSet<>();
			libraryFiles.add( dependency( "org.hibernate:hibernate-search-orm:" + currentVersion ) );
			libraryFiles.add( dependency( "org.hibernate:hibernate-search-engine:" + currentVersion ) );
			libraryFiles.add( dependency( "org.hibernate:hibernate-search-backend-jms:" + currentVersion ) );
			libraryFiles.add( dependency( "org.hibernate:hibernate-search-serialization-java:" + currentVersion ) );
			libraryFiles.add( dependency( "org.apache.lucene:lucene-core:" + VersionTestHelper.getDependencyVersionLucene() ) );
			libraryFiles.add( dependency( "org.apache.lucene:lucene-analyzers-common:" + VersionTestHelper.getDependencyVersionLucene()) );
			libraryFiles.add( dependency( "org.hibernate.common:hibernate-commons-annotations:" + VersionTestHelper.getDependencyVersionHibernateCommonsAnnotations() ) );
			return libraryFiles.toArray( new File[0] );
		}

		private static File dependency(String mavenName) {
			return Maven.resolver()
					.resolve( mavenName )
					.withoutTransitivity()
					.asSingleFile();
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
