/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentPublicParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentRelocationParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isProjectDeploySkipped;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

@Named("dependencyManagementIncludesAllGroupIdArtifactsRule") // rule name - must start with lowercase character
public class DependencyManagementIncludesAllGroupIdArtifactsRule extends AbstractEnforcerRule {
	/**
	 * See <a href="https://central.sonatype.org/search/rest-api-guide/">Maven Central REST API</a>
	 */
	private static final String BASE_URL_FORMAT =
			"https://search.maven.org/solrsearch/select?q=%s&core=gav&start=%d&rows=%d&wt=json";
	private static final String MAVEN_STATUS_URL = "https://status.maven.org/api/v2/summary.json";
	private static final int ROWS_PER_PAGE = 100;
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_DELAY_SECONDS = 2;
	private static final String GAV_FORMAT = "%s:%s:%s";


	// Inject needed Maven components
	@Inject
	private MavenSession session;

	/**
	 * Set of project to scan for dependencies. If empty -- all published project are included.
	 */
	private Set<Dependency> includedProjects = new HashSet<>();

	/**
	 * Set of extra dependencies to include in queries and hence the ones expected in the pom
	 */
	private Set<Dependency> includedDependencies = new HashSet<>();

	/**
	 * set of "found" dependencies that can be ignored.
	 */
	private Set<Dependency> dependenciesToSkip = new HashSet<>();

	public void execute() throws EnforcerRuleException {
		Set<String> dependencies = session.getCurrentProject()
				.getDependencyManagement()
				.getDependencies()
				.stream()
				.map( DependencyManagementIncludesAllGroupIdArtifactsRule::dependencyString )
				.collect( Collectors.toSet() );
		Set<Pattern> skip = dependenciesToSkip.stream()
				.map( DependencyManagementIncludesAllGroupIdArtifactsRule::dependencyString )
				.map( s -> String.format( Locale.ROOT, "^%s$", s ) )
				.map( Pattern::compile )
				.collect( Collectors.toSet() );
		Set<String> include = includedProjects.stream()
				.map( DependencyManagementIncludesAllGroupIdArtifactsRule::dependencyString )
				.collect( Collectors.toSet() );

		Set<Artifact> toQuery = new TreeSet<>();
		for ( MavenProject project : session.getAllProjects() ) {
			String projectGav = String.format(
					Locale.ROOT, GAV_FORMAT, project.getGroupId(), project.getArtifactId(),
					project.getVersion()
			);
			if ( !include.isEmpty() && !include.contains( projectGav ) || shouldSkip( projectGav, skip ) ) {
				continue;
			}
			boolean publicParent = isAnyParentPublicParent( project );
			boolean relocationParent = isAnyParentRelocationParent( project );
			boolean shouldBePublished = publicParent || relocationParent;
			boolean deploySkipped = isProjectDeploySkipped( project );
			if ( shouldBePublished && !deploySkipped ) {
				for ( Dependency dependency : project.getDependencies() ) {
					if ( "test".equals( dependency.getScope() ) ) {
						continue;
					}
					toQuery.add( new Artifact( dependency.getGroupId(), null, dependency.getVersion() ) );
				}
			}
		}

		for ( Dependency filter : includedDependencies ) {
			toQuery.add( new Artifact( filter.getGroupId(), filter.getVersion(), filter.getArtifactId() ) );
		}

		getLog().info( "Will attempt to resolve the artifacts for the following groups: " + toQuery );

		final Gson gson = new GsonBuilder().create();
		Set<Artifact> foundArtifacts = new TreeSet<>();

		StatusSummary mavenStatus = fetch( gson, MAVEN_STATUS_URL, StatusSummary.class, StatusSummary::empty );

		if ( mavenStatus.isSearchAvailable() ) {
			for ( Artifact filter : toQuery ) {
				StringBuilder queryBuilder = new StringBuilder();
				queryBuilder.append( "g:" ).append( encodeValue( filter.g ) );

				if ( filter.a != null && !filter.a.trim().isEmpty() ) {
					queryBuilder.append( "+AND+a:" ).append( encodeValue( filter.a ) );
				}
				if ( filter.v != null && !filter.v.trim().isEmpty() ) {
					queryBuilder.append( "+AND+v:" ).append( encodeValue( filter.v ) );
				}

				int start = 0;
				do {
					String url = String.format( Locale.ROOT, BASE_URL_FORMAT, queryBuilder, start, ROWS_PER_PAGE );
					SearchResponse response = fetch( gson, url, SearchResponse.class, SearchResponse::empty );
					foundArtifacts.addAll( response.response.docs );
					if ( response.response.start + response.response.docs.size() >= response.response.numFound ) {
						break;
					}
					start += ROWS_PER_PAGE;
				}
				while ( true );
			}
		}
		else {
			getLog().warn( "maven.search.org is down. Skipping search of artifacts. Check incomplete." );
		}

		List<String> problems = new ArrayList<>();
		for ( Artifact artifact : foundArtifacts ) {
			String toCheck = artifact.formattedString();
			if ( shouldSkip( toCheck, skip ) ) {
				continue;
			}
			if ( !dependencies.remove( toCheck ) ) {
				// The artifact is NOT in the dependencies
				problems.add( "`" + toCheck + "` is missing from the dependency management section" );
			}
		}

		if ( !problems.isEmpty() ) {
			throw new EnforcerRuleException( String.join( ";\n", problems ) );
		}
	}

	private static String dependencyString(Dependency d) {
		return String.format( Locale.ROOT, GAV_FORMAT, d.getGroupId(), d.getArtifactId(), d.getVersion() );
	}
	
	private static boolean shouldSkip(String gav, Set<Pattern> skipPatterns) {
		for ( Pattern skipPattern : skipPatterns ) {
			if ( skipPattern.matcher( gav ).matches() ) {
				return true;
			}
		}
		return false;
	}

	private <T> T fetch(Gson gson, String url, Class<T> klass, Supplier<T> empty) throws EnforcerRuleException {
		for ( int i = 0; i < MAX_RETRIES; i++ ) {
			try (
					var stream = URI.create( url ).toURL().openStream(); var isr = new InputStreamReader( stream );
					var reader = new JsonReader( isr )
			) {
				getLog().info( "Fetching from " + url );
				return gson.fromJson( reader, klass );
			}
			catch (IOException e) {
				getLog().warn( "Fetching from " + url + " failed. Retrying in " + RETRY_DELAY_SECONDS
						+ "s... (Attempt " + ( i + 1 ) + "/" + ( MAX_RETRIES ) + "): " + e.getMessage() );
				try {
					TimeUnit.SECONDS.sleep( RETRY_DELAY_SECONDS );
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new EnforcerRuleException( ie );
				}
			}
		}
		getLog().warn( "Fetching from " + url + " failed after " + ( MAX_RETRIES ) + " attempts." );
		return empty.get();
	}

	private String encodeValue(String value) {
		return URLEncoder.encode( value, StandardCharsets.UTF_8 );
	}

	private static class Artifact implements Comparable<Artifact> {
		String g;
		String a;
		String v;

		public Artifact(String g, String a, String v) {
			this.g = g;
			this.a = a;
			this.v = v;
		}

		public Artifact() {
		}

		public String formattedString() {
			return String.format( Locale.ROOT, GAV_FORMAT, g, a, v );
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Artifact artifact = (Artifact) o;
			return Objects.equals( g, artifact.g ) && Objects.equals( a, artifact.a ) && Objects.equals( v, artifact.v );
		}

		@Override
		public int hashCode() {
			return Objects.hash( g, a, v );
		}

		@Override
		public int compareTo(Artifact o) {
			if ( o == null ) {
				return 1;
			}
			return formattedString().compareTo( o.formattedString() );
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			if ( g != null ) {
				sb.append( g );
			}
			else {
				sb.append( "-" );
			}
			sb.append( ':' );

			if ( a != null ) {
				sb.append( a );
			}
			else {
				sb.append( "-" );
			}
			sb.append( ':' );
			if ( v != null ) {
				sb.append( v );
			}
			else {
				sb.append( "-" );
			}
			return sb.toString();
		}
	}

	private static class SearchResponse {
		ResponseData response;

		static SearchResponse empty() {
			SearchResponse res = new SearchResponse();
			res.response = new ResponseData();
			res.response.numFound = 0;
			res.response.start = 0;
			res.response.docs = List.of();
			return res;
		}
	}

	public static class ResponseData {
		int numFound;
		int start;
		List<Artifact> docs;
	}

	private static class StatusSummary {
		public List<Component> components;

		private static class Component {
			public String id;
			public String name;
			public String status; // e.g., "operational", "degraded_performance", "major_outage"
		}

		boolean isSearchAvailable() {
			for ( Component component : components ) {
				if ( component.name != null && component.name.contains( "search.maven.org" ) ) {
					return "operational".equalsIgnoreCase( component.status );
				}
			}
			return false;
		}

		static StatusSummary empty() {
			StatusSummary statusSummary = new StatusSummary();
			statusSummary.components = List.of();
			return statusSummary;
		}
	}
}
