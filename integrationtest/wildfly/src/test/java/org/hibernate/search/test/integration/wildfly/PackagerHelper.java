/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
			.using( new RejectDependenciesStrategy(
				false, //we need some dependencies at the right version: Lucene, search-engine, etc..
				"org.hibernate:hibernate-core",
				"org.hibernate:hibernate-commons-annotations",
				"org.hibernate.javax.persistence:hibernate-jpa-2.1-api",
				"org.jboss.logging:jboss-logging") )
				.as( JavaArchive.class );
		// To debug dependencies, have it dump a zip export:
		//archive.as( ZipExporter.class ).exportTo( new File("test-app.war"), true );
	}

}
