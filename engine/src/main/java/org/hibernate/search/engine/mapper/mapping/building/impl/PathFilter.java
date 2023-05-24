/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

abstract class PathFilter {

	static PathFilter unconstrained() {
		return Unconstrained.INSTANCE;
	}

	static PathFilter of(Set<String> includedPaths, Set<String> excludedPaths) {
		if ( includedPaths == null || includedPaths.isEmpty() ) {
			if ( excludedPaths != null && !excludedPaths.isEmpty() ) {
				return new ExcludePathFilter( excludedPaths );
			}
			return unconstrained();
		}

		// The included paths in the filter
		Set<String> actualIncludedPaths = new HashSet<>();

		for ( String path : includedPaths ) {
			actualIncludedPaths.add( path );
			// Also add paths leading to this path (so that object nodes are not excluded)
			addSubPathsFromRoot( actualIncludedPaths, path );
		}

		return new IncludePathFilter( actualIncludedPaths );
	}

	/**
	 * Paths to be included even when the default behavior is to exclude paths.
	 */
	protected final Set<String> paths;

	private PathFilter(Set<String> paths) {
		this.paths = paths;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "paths=" + paths
				+ "]";
	}

	boolean isExplicitlyIncluded(String relativePath) {
		return false;
	}
	boolean isExplicitlyExcluded(String relativePath) {
		return false;
	}

	abstract boolean isAnyPathExplicitlyIncluded();

	private static class IncludePathFilter extends PathFilter {
		private IncludePathFilter(Set<String> paths) {
			super( paths );
		}

		@Override
		boolean isExplicitlyIncluded(String relativePath) {
			return paths.contains( relativePath );
		}

		@Override
		boolean isAnyPathExplicitlyIncluded() {
			return true;
		}
	}

	private static class ExcludePathFilter extends PathFilter {
		private ExcludePathFilter(Set<String> paths) {
			super( paths );
		}

		@Override
		boolean isExplicitlyExcluded(String relativePath) {
			return paths.contains( relativePath );
		}

		@Override
		boolean isAnyPathExplicitlyIncluded() {
			return false;
		}
	}

	private static class Unconstrained extends PathFilter {
		private static final PathFilter INSTANCE = new Unconstrained();
		private Unconstrained() {
			super( Collections.emptySet() );
		}

		@Override
		boolean isExplicitlyIncluded(String relativePath) {
			return false;
		}

		@Override
		boolean isAnyPathExplicitlyIncluded() {
			return false;
		}
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
