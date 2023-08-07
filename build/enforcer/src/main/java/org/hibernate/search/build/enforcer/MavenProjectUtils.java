/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.build.enforcer;

import org.apache.maven.project.MavenProject;

public class MavenProjectUtils {

	private static final String HIBERNATE_SEARCH_PARENT_PUBLIC = "hibernate-search-parent-public";

	private MavenProjectUtils() {
	}

	public static boolean isAnyParentPublicParent(MavenProject project) {
		return project.hasParent()
				&& ( HIBERNATE_SEARCH_PARENT_PUBLIC.equals( project.getParent().getArtifactId() )
						|| isAnyParentPublicParent( project.getParent() ) );
	}

	public static boolean isProjectNotDeployed(MavenProject project) {
		return Boolean.TRUE.toString()
				.equals( project.getProperties().getOrDefault( "skipNexusStagingDeployMojo", Boolean.FALSE ).toString() );
	}

	public static boolean isProjectSigned(MavenProject project) {
		return Boolean.FALSE.toString()
				.equals( project.getProperties().getOrDefault( "release.gpg.signing.skip", Boolean.FALSE ).toString() );
	}
}
