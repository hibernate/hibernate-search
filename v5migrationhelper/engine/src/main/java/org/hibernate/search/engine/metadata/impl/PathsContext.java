/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about the current set of paths as
 * well as tracking which paths have been encountered to validate the
 * existence of all configured paths.
 */
public class PathsContext {

	private final Map<String, Boolean> pathsEncounteredState = new LinkedHashMap<>();

	public void addIncludedPath(String path) {
		pathsEncounteredState.put( path, Boolean.FALSE );
	}

	public boolean isIncluded(DocumentFieldPath path) {
		return pathsEncounteredState.keySet().contains( path.getAbsoluteName() );
	}

	public void markEncounteredPath(DocumentFieldPath path) {
		pathsEncounteredState.put( path.getAbsoluteName(), Boolean.TRUE );
	}

	public Set<String> getEncounteredPaths() {
		return pathsEncounteredState.keySet();
	}

	public Set<String> getUnEncounteredPaths() {
		Set<String> unEncounteredPaths = new LinkedHashSet<>();
		for ( String path : pathsEncounteredState.keySet() ) {
			if ( notEncountered( path ) ) {
				unEncounteredPaths.add( path );
			}
		}
		return unEncounteredPaths;
	}

	private boolean notEncountered(String path) {
		return !pathsEncounteredState.get( path );
	}
}


