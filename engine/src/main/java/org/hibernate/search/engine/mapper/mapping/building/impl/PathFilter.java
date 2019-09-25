/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

class PathFilter {

	private static final PathFilter UNCONSTRAINED = new PathFilter( Collections.emptySet(), Collections.emptySet() );

	static PathFilter unconstrained() {
		return UNCONSTRAINED;
	}

	static PathFilter of(Set<String> paths) {
		if ( paths == null || paths.isEmpty() ) {
			return unconstrained();
		}

		// The included paths in the filter
		Set<String> includedPaths = new HashSet<>();
		// The included paths in the filter that were explicitly mentioned in "paths"
		// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
		Set<String> localIncludedPaths = new LinkedHashSet<>();

		for ( String path : paths ) {
			includedPaths.add( path );
			// Also add paths leading to this path (so that object nodes are not excluded)
			addSubPathsFromRoot( includedPaths, path );

			localIncludedPaths.add( path );
		}

		return new PathFilter( includedPaths, localIncludedPaths );
	}

	/**
	 * Paths to be included even when the default behavior is to exclude paths.
	 */
	private final Set<String> includedPaths;

	/**
	 * The {@link #includedPaths} that were included by this filter explicitly.
	 * Does not include sub-paths derived automatically, in particular
	 * ("a.b.c" => ["a", "a.b", "a.b.c"], but only "a.b.c" shows up here).
	 */
	private final Set<String> configuredIncludedPaths;

	private PathFilter(Set<String> includedPaths, Set<String> configuredIncludedPaths) {
		this.includedPaths = includedPaths;
		this.configuredIncludedPaths = configuredIncludedPaths;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "includedPaths=" + includedPaths
				+ ",localIncludedPaths=" + configuredIncludedPaths
				+ "]";
	}

	boolean isExplicitlyIncluded(String relativePath) {
		return includedPaths.contains( relativePath );
	}

	boolean isAnyPathExplicitlyIncluded() {
		return !includedPaths.isEmpty();
	}

	Set<String> getConfiguredIncludedPaths() {
		return configuredIncludedPaths;
	}

	private static void addSubPathsFromRoot(Set<String> collector, String path) {
		int afterPreviousDotIndex = 0;
		int nextDotIndex = path.indexOf( '.', afterPreviousDotIndex );
		while ( nextDotIndex >= 0 ) {
			String subPath = path.substring( 0, nextDotIndex );
			collector.add( subPath );
			afterPreviousDotIndex = nextDotIndex + 1;
			nextDotIndex = path.indexOf( '.', afterPreviousDotIndex );
		}
	}
}
