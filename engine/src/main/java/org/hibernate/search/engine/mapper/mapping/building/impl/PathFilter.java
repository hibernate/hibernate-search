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

	/**
	 * Paths to be included even when the default behavior is to exclude paths.
	 */
	private final Set<String> includedPaths;

	/**
	 * The {@link #includedPaths} that were included by this filter explicitly (not a parent filter).
	 */
	private final Set<String> localIncludedPaths;

	private PathFilter(Set<String> includedPaths, Set<String> localIncludedPaths) {
		this.includedPaths = includedPaths;
		this.localIncludedPaths = localIncludedPaths;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "includedPaths=" + includedPaths
				+ ",localIncludedPaths=" + localIncludedPaths
				+ "]";
	}

	boolean isExplicitlyIncluded(String relativePath) {
		return includedPaths.contains( relativePath );
	}

	boolean isAnyPathExplicitlyIncluded() {
		return !includedPaths.isEmpty();
	}

	Set<String> getLocalIncludedPaths() {
		return localIncludedPaths;
	}

	PathFilter increaseDepth(String relativePrefix) {
		Set<String> newIncludedPaths = new HashSet<>();
		Set<String> newLocalIncludedPaths = new LinkedHashSet<>();

		// Only keep paths that start with the given prefix.
		filterPathsByPrefix( newIncludedPaths, this.includedPaths, relativePrefix );
		filterPathsByPrefix( newLocalIncludedPaths, this.localIncludedPaths, relativePrefix );

		return new PathFilter( newIncludedPaths, newLocalIncludedPaths );
	}

	PathFilter combine(Set<String> includedPathsToCombine,
			boolean includePathsToCombineByDefault, boolean includePathsOfThisByDefault) {
		// The included paths in the new, combined filter
		Set<String> combinedIncludedPaths = new HashSet<>();
		// The included paths in the new, combined filter that were part of includedPathsToCombine
		// (and not inherited from this filter)
		// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
		Set<String> combinedLocalIncludedPaths = new LinkedHashSet<>();

		/*
		 * Add the new included paths to the combined filter's included paths,
		 * provided they are not filtered out by the current filter.
		 */
		for ( String path : includedPathsToCombine ) {
			if ( includePathsToCombineByDefault || this.isExplicitlyIncluded( path ) ) {
				combinedIncludedPaths.add( path );
				// Also add paths leading to this path (so that object nodes are not excluded)
				addSubPathsFromRoot( combinedIncludedPaths, path );

				// Consider the other filter's paths as the local included paths in the combined filter
				combinedLocalIncludedPaths.add( path );
			}
		}

		/*
		 * Add the current filter's included paths to the combined filter's included paths,
		 * provided they are not filtered out by the new included paths.
		 */
		PathFilter otherFilter = new PathFilter( includedPathsToCombine, includedPathsToCombine );
		for ( String path : this.includedPaths ) {
			if ( includePathsOfThisByDefault || otherFilter.isExplicitlyIncluded( path ) ) {
				combinedIncludedPaths.add( path );
			}
		}

		return new PathFilter(
				combinedIncludedPaths,
				combinedLocalIncludedPaths
		);
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

	private static void filterPathsByPrefix(Set<String> collector, Set<String> paths, String prefix) {
		int prefixLength = prefix.length();
		for ( String path : paths ) {
			if ( path.startsWith( prefix ) ) {
				String pathRelativeToNewDepth = path.substring( prefixLength );
				collector.add( pathRelativeToNewDepth );
			}
		}
	}
}
