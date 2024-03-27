/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import static org.hibernate.search.build.enforcer.MavenProjectUtils.DEPLOY_SKIP;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentPublicParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentRelocationParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isProjectDeploySkipped;

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

@Named("dependencyManagementIncludesAllPublicArtifactsRule") // rule name - must start with lowercase character
public class DependencyManagementIncludesAllPublicArtifactsRule extends AbstractEnforcerRule {

	// Inject needed Maven components
	@Inject
	private MavenSession session;

	public void execute() throws EnforcerRuleException {
		Map<String, Dependency> dependencies = session.getCurrentProject()
				.getDependencyManagement()
				.getDependencies()
				.stream()
				.collect( Collectors.toMap( Dependency::getArtifactId, Function.identity() ) );

		List<String> problems = new ArrayList<>();

		for ( MavenProject project : session.getAllProjects() ) {
			boolean publicParent = isAnyParentPublicParent( project );
			boolean relocationParent = isAnyParentRelocationParent( project );
			boolean shouldBePublished = publicParent || relocationParent;
			boolean deploySkipped = isProjectDeploySkipped( project );
			if ( dependencies.remove( project.getArtifactId() ) == null ) {
				// The project is NOT in the dependencies

				if ( shouldBePublished && !deploySkipped ) {
					problems.add( "`" + project.getGroupId() + ":" + project.getArtifactId()
							+ "` is missing from the dependency management section." );
				}
			}
			else {
				// The project IS in the dependencies

				if ( !shouldBePublished || deploySkipped ) {
					problems.add( "`" + project.getGroupId() + ":" + project.getArtifactId()
							+ "` either is misconfigured, or it is not published so it should not be in the dependency management section:"
							+ " [parents include '" + MavenProjectUtils.HIBERNATE_SEARCH_PARENT_PUBLIC
							+ "' = " + publicParent
							+ " [parents include '" + MavenProjectUtils.HIBERNATE_SEARCH_PARENT_RELOCATION
							+ "' = " + relocationParent
							+ ", Maven property '" + DEPLOY_SKIP + "' = " + deploySkipped + "]" );
				}

			}
		}

		for ( Dependency dependency : dependencies.values() ) {
			problems.add( "`" + dependency.getGroupId() + ":" + dependency.getArtifactId()
					+ "` is not a Hibernate Search artifact so it should not be in the dependency management section:" );
		}

		if ( !problems.isEmpty() ) {
			throw new EnforcerRuleException( String.join( ";\n", problems ) );
		}
	}
}
