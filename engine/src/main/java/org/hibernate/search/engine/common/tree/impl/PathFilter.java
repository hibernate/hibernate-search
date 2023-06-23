/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.tree.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class PathFilter {

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
	 * Paths that filter works with.
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

	abstract boolean isExplicitlyIncluded(String relativePath);

	abstract boolean isExplicitlyExcluded(String relativePath);

	abstract boolean isPotentiallyExcluded(String path);

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
		boolean isExplicitlyExcluded(String relativePath) {
			return false;
		}

		@Override
		boolean isPotentiallyExcluded(String path) {
			// no exclude paths here. and if the "cycles are broken by it" we'll detect it differently anyway
			return false;
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
		boolean isExplicitlyIncluded(String relativePath) {
			return false;
		}

		@Override
		boolean isExplicitlyExcluded(String relativePath) {
			return paths.contains( relativePath );
		}

		@Override
		boolean isPotentiallyExcluded(String pathToTest) {
			for ( String excludePath : paths ) {
				if (
					// Exact match -- great! it means we will definitely exclude our path-to-test.
				excludePath.equals( pathToTest )
						// Otherwise, make sure that we have an exclude path that starts with our path-to-test
						|| excludePath.startsWith( pathToTest )
								// and make sure that our matched path actually is a path of complete object names,
								// i.e. in excludePath, there is some other object name following our path-to-test:
								&& excludePath.startsWith( ".", pathToTest.length() ) ) {
					return true;
				}
			}

			return false;
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
		boolean isExplicitlyExcluded(String relativePath) {
			return false;
		}

		@Override
		boolean isPotentiallyExcluded(String path) {
			// no constraints - so can't be excluded
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
