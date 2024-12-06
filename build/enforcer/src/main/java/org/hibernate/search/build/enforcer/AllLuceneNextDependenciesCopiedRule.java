/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

@Named("allLuceneNextDependenciesCopiedRule") // rule name - must start with lowercase character
public class AllLuceneNextDependenciesCopiedRule extends AbstractEnforcerRule {

	private static final String DEPENDENCY_PLUGIN = "org.apache.maven.plugins:maven-dependency-plugin";
	private static final String EXECUTION_COPY = "copy-dependencies-lucene-next";
	private static final String LUCENE_NEXT_ARTIFACT_ID = "hibernate-search-backend-lucene-next";
	// Inject needed Maven components
	@Inject
	private MavenSession session;

	@Inject
	private ProjectDependenciesResolver dependenciesResolver;

	/**
	 * Rule parameter as list of items.
	 */
	private Set<Dependency> dependenciesToSkip;

	public void execute() throws EnforcerRuleException {
		Plugin plugin = session.getCurrentProject().getPlugin( DEPENDENCY_PLUGIN );
		if ( plugin == null ) {
			throw new EnforcerRuleException( "Project %s:%s does not configure the Dependency plugin (%s)!"
					.formatted( session.getCurrentProject().getGroupId(), session.getCurrentProject().getArtifactId(),
							DEPENDENCY_PLUGIN
					) );
		}

		PluginExecution execution = plugin.getExecutionsAsMap().get( EXECUTION_COPY );
		if ( execution == null ) {
			throw new EnforcerRuleException( "Project %s:%s does not configure the Dependency plugin (%s) execution \"%s\"!"
					.formatted( session.getCurrentProject().getGroupId(), session.getCurrentProject().getArtifactId(),
							DEPENDENCY_PLUGIN, EXECUTION_COPY
					) );
		}

		if ( execution.getConfiguration() instanceof Xpp3Dom configuration ) {
			Xpp3Dom artifactItems = configuration.getChild( "artifactItems" );
			if ( artifactItems == null ) {
				throw new EnforcerRuleException(
						"Project %s:%s does not specify the Javadoc plugin (%s) execution \"%s\" sourcepath configuration attribute!"
								.formatted( session.getCurrentProject().getGroupId(),
										session.getCurrentProject().getArtifactId(),
										DEPENDENCY_PLUGIN, EXECUTION_COPY
								) );
			}

			MavenProject luceneNextProject = session.getAllProjects().stream()
					.filter( p -> "jar".equals( p.getPackaging() ) && LUCENE_NEXT_ARTIFACT_ID.equals( p.getArtifactId() ) )
					.findFirst()
					.orElseThrow(
							() -> new EnforcerRuleException( "%s is not available, cannot determine required dependencies."
									.formatted( LUCENE_NEXT_ARTIFACT_ID ) ) );

			List<org.eclipse.aether.graph.Dependency> dependencies = resolveDependencies( luceneNextProject );
			List<Dependency> redundantDependencies = new ArrayList<>();


			for ( Xpp3Dom artifact : artifactItems.getChildren() ) {
				if ( !dependencies
						.removeIf( dep -> dep.getArtifact().getGroupId().equals( artifact.getChild( "groupId" ).getValue() )
								&& dep.getArtifact().getArtifactId().equals( artifact.getChild( "artifactId" ).getValue() )
								&& dep.getArtifact().getVersion().equals( artifact.getChild( "version" ).getValue() ) ) ) {
					Dependency dependency = new Dependency();
					dependency.setGroupId( artifact.getChild( "groupId" ).getValue() );
					dependency.setArtifactId( artifact.getChild( "artifactId" ).getValue() );
					dependency.setVersion( artifact.getChild( "version" ).getValue() );
					redundantDependencies.add( dependency );
				}
			}

			if ( !dependencies.isEmpty() ) {
				throw new EnforcerRuleException(
						"Some Lucene next backend dependencies are missing: %s".formatted( dependencies ) );
			}
			if ( !redundantDependencies.isEmpty() ) {
				throw new EnforcerRuleException(
						"Some Lucene next backend dependencies are redundant: %s".formatted( redundantDependencies ) );
			}
		}
	}

	private List<org.eclipse.aether.graph.Dependency> resolveDependencies(MavenProject luceneNextProject) {
		try {
			DependencyResolutionResult result = dependenciesResolver
					.resolve( new DefaultDependencyResolutionRequest( luceneNextProject, session.getRepositorySession() )
							.setResolutionFilter(
									new AndDependencyFilter(
											// we skip test dependencies
											new ScopeDependencyFilter( "test" ),
											// and the ones we explicitly asked to skip:
											new ExclusionsDependencyFilter( dependenciesToSkip.stream()
													.map( d -> d.getGroupId() + ":" + d.getArtifactId() ).toList() )
									) ) );
			return result.getDependencies();
		}
		catch (DependencyResolutionException e) {
			throw new RuntimeException( e );
		}
	}
}
