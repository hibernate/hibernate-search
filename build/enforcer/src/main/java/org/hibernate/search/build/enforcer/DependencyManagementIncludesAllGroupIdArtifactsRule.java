/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

@Named("dependencyManagementIncludesAllGroupIdArtifactsRule") // rule name - must start with lowercase character
public class DependencyManagementIncludesAllGroupIdArtifactsRule extends AbstractEnforcerRule {
	/**
	 * See <a href="https://central.sonatype.org/search/rest-api-guide/">Maven Central REST API</a>
	 */
	private static final String BASE_URL_FORMAT =
			"https://search.maven.org/solrsearch/select?q=%s&core=gav&start=%d&rows=%d&wt=json";
	private static final int ROWS_PER_PAGE = 100;
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_DELAY_SECONDS = 2;


	// Inject needed Maven components
	@Inject
	private MavenSession session;

	/**
	 * Rule parameter as list of items.
	 */
	private Set<Dependency> includedDependencyFilters = new HashSet<>();

	/**
	 * Rule parameter as list of items.
	 */
	private Set<Dependency> dependenciesToSkip = new HashSet<>();

	public void execute() throws EnforcerRuleException {
		Set<String> dependencies = session.getCurrentProject()
				.getDependencyManagement()
				.getDependencies()
				.stream()
				.map( d -> String.format( Locale.ROOT, "%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion() ) )
				.collect( Collectors.toSet() );
		Set<String> skip = dependenciesToSkip.stream()
				.map( d -> String.format( Locale.ROOT, "%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion() ) )
				.collect( Collectors.toSet() );

		final Gson gson = new GsonBuilder().create();
		Set<Artifact> foundArtifacts = new HashSet<>();

		for ( Dependency filter : includedDependencyFilters ) {
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append( "g:" ).append( encodeValue( filter.getGroupId() ) );

			if ( filter.getArtifactId() != null && !filter.getArtifactId().trim().isEmpty() ) {
				queryBuilder.append( "+AND+a:" ).append( encodeValue( filter.getArtifactId() ) );
			}
			if ( filter.getVersion() != null && !filter.getVersion().trim().isEmpty() ) {
				queryBuilder.append( "+AND+v:" ).append( encodeValue( filter.getVersion() ) );
			}


			int start = 0;
			do {
				String url = String.format( Locale.ROOT, BASE_URL_FORMAT, queryBuilder, start, ROWS_PER_PAGE );
				SearchResponse response = fetch( gson, url );
				foundArtifacts.addAll( response.response.docs );
				if ( response.response.start + response.response.docs.size() >= response.response.numFound ) {
					break;
				}
			}
			while ( true );
		}

		List<String> problems = new ArrayList<>();
		for ( Artifact artifact : foundArtifacts ) {
			String toCheck = artifact.formattedString();
			if ( skip.contains( toCheck ) ) {
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

	private SearchResponse fetch(Gson gson, String url) throws EnforcerRuleException {
		for ( int i = 0; i < MAX_RETRIES; i++ ) {
			try (
					var stream = URI.create( url ).toURL().openStream(); var isr = new InputStreamReader( stream );
					var reader = new JsonReader( isr )
			) {
				getLog().info( "Fetching included artifacts from " + url );
				return gson.fromJson( reader, SearchResponse.class );
			}
			catch (IOException e) {
				getLog().warn( "Fetching artifacts from " + url + " failed Retrying in " + RETRY_DELAY_SECONDS
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
		throw new EnforcerRuleException(
				"Fetching the artifacts from " + url + " failed after " + ( MAX_RETRIES ) + " attempts." );
	}

	private String encodeValue(String value) {
		return URLEncoder.encode( value, StandardCharsets.UTF_8 );
	}

	private static class Artifact {
		String g;
		String a;
		String v;

		public String formattedString() {
			return String.format( Locale.ROOT, "%s:%s:%s", g, a, v );
		}
	}

	private static class SearchResponse {
		ResponseData response;
	}

	public static class ResponseData {
		int numFound;
		int start;
		List<Artifact> docs;
	}
}
