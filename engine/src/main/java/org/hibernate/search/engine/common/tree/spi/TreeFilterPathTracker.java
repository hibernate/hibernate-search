/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.tree.spi;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;

/**
 * A tracker for paths actually affected by a {@link TreeFilterDefinition}.
 * <p>
 * Used to detect invalid configuration in a filter definition,
 * for example useless includePaths.
 */
public final class TreeFilterPathTracker {

	private final TreeFilterDefinition definition;

	/**
	 * The {@code paths} that were encountered.
	 */
	// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
	private final Map<String, Boolean> encounteredFieldPaths = new LinkedHashMap<>();

	public TreeFilterPathTracker(TreeFilterDefinition definition) {
		this.definition = definition;
	}

	public Set<String> uselessIncludePaths() {
		Set<String> uselessIncludePaths = new LinkedHashSet<>();
		for ( String path : definition.includePaths() ) {
			Boolean included = encounteredFieldPaths.get( path );
			if ( included == null /* not encountered */ || !included ) {
				// An "includePaths" filter that does not result in inclusion is useless
				uselessIncludePaths.add( path );
			}
		}
		return uselessIncludePaths;
	}

	public Set<String> uselessExcludePaths() {
		Set<String> uselessExcludePaths = new LinkedHashSet<>();
		for ( String path : definition.excludePaths() ) {
			if (
				// Since it's exclude we should always get a FALSE value from this map for such key.
			// Hence, we only want to check for fields that we never encountered i.e. some typos in property names or just some "pointless" text :)
			//
			// Alternatively, if a property was "explicitly" excluded by a child, which would make adding it to excludes
			// on this level redundant (or in other words such that references a "missing" property) we will not get
			// such path marked at all since we stop the recursion early in the filter.
			!encounteredFieldPaths.containsKey( path )
			) {
				uselessExcludePaths.add( path );
			}
		}
		return uselessExcludePaths;
	}

	public Set<String> encounteredFieldPaths() {
		return encounteredFieldPaths.keySet();
	}

	public void markAsEncountered(String relativePath, boolean includedByThis) {
		encounteredFieldPaths.merge(
				relativePath, includedByThis,
				(included1, included2) -> included1 || included2
		);
	}
}
