/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.build.enforcer;

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

	private static final String HIBERNATE_SEARCH_PARENT_PUBLIC = "hibernate-search-parent-public";
	// Inject needed Maven components
	@Inject
	private MavenSession session;

	public void execute() throws EnforcerRuleException {

		Map<String, Dependency> dependencies = session.getCurrentProject()
				.getDependencyManagement()
				.getDependencies()
				.stream()
				.collect( Collectors.toMap( Dependency::getArtifactId, Function.identity() ) );

		for ( MavenProject project : session.getAllProjects() ) {
			if ( isAnyParentPublicParent( project )
					&& !isProjectNotDeployed( project )
					&& ( dependencies.remove( project.getArtifactId() ) == null ) ) {
				throw new EnforcerRuleException(
						"`" + project.getGroupId() + ":" + project.getArtifactId()
								+ "` is missing in the dependency management." );
			}
		}

		if ( !dependencies.isEmpty() ) {
			throw new EnforcerRuleException( "Unexpected dependencies listed in the dependency management section: "
					+ dependencies.values() );
		}
	}

	private boolean isAnyParentPublicParent(MavenProject project) {
		return project.hasParent()
				&& ( HIBERNATE_SEARCH_PARENT_PUBLIC.equals( project.getParent().getArtifactId() )
						|| isAnyParentPublicParent( project.getParent() ) );
	}

	private boolean isProjectNotDeployed(MavenProject project) {
		return Boolean.TRUE.toString()
				.equals( project.getProperties().getOrDefault( "skipNexusStagingDeployMojo", Boolean.FALSE ).toString() );
	}
}
