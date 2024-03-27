/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import org.apache.maven.project.MavenProject;

public class MavenProjectUtils {

	public static final String HIBERNATE_SEARCH_PARENT_PUBLIC = "hibernate-search-parent-public";
	public static final String HIBERNATE_SEARCH_PARENT_INTEGRATION_TEST = "hibernate-search-parent-integrationtest";
	public static final String HIBERNATE_SEARCH_PARENT_RELOCATION = "hibernate-search-parent-relocation";
	public static final String DEPLOY_SKIP = "deploy.skip";

	private MavenProjectUtils() {
	}

	public static boolean isAnyParentPublicParent(MavenProject project) {
		return project.hasParent()
				&& ( HIBERNATE_SEARCH_PARENT_PUBLIC.equals( project.getParent().getArtifactId() )
				|| isAnyParentPublicParent( project.getParent() ) );
	}

	public static boolean isAnyParentRelocationParent(MavenProject project) {
		return project.hasParent()
				&& ( HIBERNATE_SEARCH_PARENT_RELOCATION.equals( project.getParent().getArtifactId() )
				|| isAnyParentRelocationParent( project.getParent() ) );
	}

	public static boolean isAnyParentIntegrationTestParent(MavenProject project) {
		return project.hasParent()
				&& ( HIBERNATE_SEARCH_PARENT_INTEGRATION_TEST.equals( project.getParent().getArtifactId() )
				|| isAnyParentIntegrationTestParent( project.getParent() ) );
	}

	public static boolean isProjectDeploySkipped(MavenProject project) {
		return Boolean.TRUE.toString()
				.equals( project.getProperties().getOrDefault( DEPLOY_SKIP, Boolean.FALSE ).toString() );
	}

	public static boolean isProjectJacocoSkipped(MavenProject project) {
		return Boolean.TRUE.toString()
				.equals( project.getProperties().getOrDefault( "jacoco.skip", Boolean.FALSE ).toString() );
	}
}
