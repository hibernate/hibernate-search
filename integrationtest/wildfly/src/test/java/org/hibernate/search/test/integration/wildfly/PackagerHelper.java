/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly;

import org.hibernate.search.engine.Version;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.RejectDependenciesStrategy;


/**
 * To allow some of our integration tests to be deployed in a container without
 * also bundling ShrinkWrap, we need to avoid explicit imports from ShrinkWrap:
 * delegate to static helpers from this class.
 *
 * @author Sanne Grinovero
 * @since 5.0
 */
public class PackagerHelper {

	private PackagerHelper() {
		//not meant to be instantiated
	}

	/**
	 * Packages which are never included among transitive dependencies of the below methods.
	 */
	private static String[] exclusions = new String[] {
		"org.hibernate:hibernate-core",
		"org.hibernate.common:hibernate-commons-annotations",
		"org.hibernate.javax.persistence:hibernate-jpa-2.1-api",
		"org.jboss.logging:jboss-logging",
		"org.jboss.logging:jboss-logging-processor",
		"dom4j:dom4j",
		"xml-apis:xml-apis",
		"org.jboss:jandex",
		"antlr:antlr",
		"org.apache.geronimo.specs:geronimo-jta_1.1_spec",
		"org.javassist:javassist",
		"org.jboss.jdeparser:jdeparser",
		"com.sun:tools" //Always exclude this as it's not available on JDK9
	};

	/**
	 * Returns the set of dependencies defined as org.hibernate:hibernate-search-orm at
	 * the version being built. We use transitive dependencies to include the version
	 * of hibernate-search-engine and Apache Lucene at the currently built version, but
	 * then need to exclude the packages already provided by the application server such
	 * as Hibernate ORM.
	 *
	 * @return the set of dependencies a user would need to bundle in a web app
	 */
	public static JavaArchive[] hibernateSearchLibraries() {
		String currentVersion = Version.getVersionString();
		return Maven.resolver()
			.resolve( "org.hibernate:hibernate-search-orm:" + currentVersion )
			// we need some dependencies at the right version: Lucene, search-engine, etc..
			.using( new RejectDependenciesStrategy( false, exclusions ) )
			.as( JavaArchive.class );
		// To debug dependencies, have it dump a zip export:
		//archive.as( ZipExporter.class ).exportTo( new File("test-app.war"), true );
	}

	public static JavaArchive[] hibernateSearchTestingLibraries() {
		String currentVersion = Version.getVersionString();
		return Maven.resolver()
			.resolve( "org.hibernate:hibernate-search-testing:" + currentVersion )
			.using( new RejectDependenciesStrategy( false, exclusions ) )
			.as( JavaArchive.class );
	}

}
