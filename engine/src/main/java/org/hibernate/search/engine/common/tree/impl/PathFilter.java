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
			for ( String path : paths ) {
				if ( relativePath.startsWith( path ) ) {
					return true;
				}
			}
			return paths.contains( relativePath );
		}

		@Override
		boolean isPotentiallyExcluded(String path) {
			for ( String excludePath : paths ) {
				if ( excludePath.startsWith( path ) ) {
					String remainingPath = excludePath.substring( path.length() );
					// we want to check that we are not "cutting" a part of a property.
					// for example if we have a path causing a problem as `node1.node2` but our filter is defined as `node1.node2WithSomeSuffix`
					// we want to say that `node1.node2` is not excluded:
					// If `path` is empty it means that we have a cycle of a single indexed-embedded as `a.a.`
					// In such case we allow it to continue and at least try to add one self element so that we don't need to pass that relative path here
					// and see if the filter maybe starts with it ...
					if ( path.isEmpty() || remainingPath.isEmpty() || remainingPath.startsWith( "." ) ) {
						return true;
					}
				}
				// If we'd wanted to make things work for prefixes without dots in the end, we'd need to modify the above ^ conditions.
				// since there we are checking that the remainingPath starts with a dot, when in case of prefixes-with-no-dots - there won't be a dot....
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
