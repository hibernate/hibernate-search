/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentPublicParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentRelocationParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isProjectDeploySkipped;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

@Named("dependencyManagementIncludesAllPublicArtifactsRule") // rule name - must start with lowercase character
public class DependencyManagementIncludesAllPublicArtifactsRule extends AbstractEnforcerRule {

	// Inject needed Maven components
	@Inject
	private MavenSession session;
	@Inject
	private ProjectDependenciesResolver projectDependenciesResolver;

	private Set<Dependency> excludes = new HashSet<>();

	public void execute() throws EnforcerRuleException {
		Set<String> dependencies = session.getCurrentProject()
				.getDependencyManagement()
				.getDependencies()
				.stream()
				.map( Dependency::getArtifactId )
				.collect( Collectors.toSet() );
		Set<String> projectsToSkip = excludes.stream()
				.map( Dependency::getArtifactId )
				.collect( Collectors.toSet() );

		List<String> problems = new ArrayList<>();

		for ( MavenProject project : session.getAllProjects() ) {
			if ( projectsToSkip.contains( project.getArtifactId() ) ) {
				continue;
			}
			boolean publicParent = isAnyParentPublicParent( project );
			boolean relocationParent = isAnyParentRelocationParent( project );
			boolean shouldBePublished = publicParent || relocationParent;
			boolean deploySkipped = isProjectDeploySkipped( project );
			if ( !dependencies.remove( project.getArtifactId() ) ) {
				// The project is NOT in the dependencies

				if ( shouldBePublished && !deploySkipped ) {
					problems.add( "`" + project.getGroupId() + ":" + project.getArtifactId()
							+ "` is missing from the dependency management section." );
				}
			}
		}

		if ( !problems.isEmpty() ) {
			throw new EnforcerRuleException( String.join( ";\n", problems ) );
		}
	}
}
