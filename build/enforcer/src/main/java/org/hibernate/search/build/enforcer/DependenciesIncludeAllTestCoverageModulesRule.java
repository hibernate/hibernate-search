/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentIntegrationTestParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentPublicParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isProjectJacocoSkipped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

@Named("dependenciesIncludeAllTestCoverageModules") // rule name - must start with lowercase character
public class DependenciesIncludeAllTestCoverageModulesRule extends AbstractEnforcerRule {

	// Inject needed Maven components
	@Inject
	private MavenSession session;

	/**
	 * Specify the test modules that will be ignored.
	 */
	private List<String> excludes = null;

	public void execute() throws EnforcerRuleException {
		Map<String, Dependency> dependencies = session.getCurrentProject()
				.getDependencies()
				.stream()
				.collect( Collectors.toMap( Dependency::getArtifactId, Function.identity() ) );

		List<String> problems = new ArrayList<>();

		for ( MavenProject project : session.getAllProjects() ) {
			if ( project.getArtifactId().equals( session.getCurrentProject().getArtifactId() ) ) {
				// We don't expect a project to depend on itself.
				continue;
			}
			if ( excludes != null && excludes.contains( project.getArtifactId() ) ) {
				continue;
			}
			if ( !"jar".equals( project.getPackaging() ) ) {
				// We don't care about non-JAR modules (those don't have any tests)
				continue;
			}
			if ( !isAnyParentPublicParent( project ) && !isAnyParentIntegrationTestParent( project ) ) {
				// We don't care about non-public, non-IT modules
				continue;
			}
			if ( isProjectJacocoSkipped( project ) ) {
				// We don't care about modules whose coverage gets ignored
				continue;
			}
			if ( dependencies.remove( project.getArtifactId() ) == null ) {
				// The project is NOT in the dependencies
				problems.add( "`" + project.getGroupId() + ":" + project.getArtifactId()
						+ "` is missing from the 'dependencies' section." );
			}
		}

		if ( !problems.isEmpty() ) {
			throw new EnforcerRuleException( String.join( ";\n", problems ) );
		}
	}
}
