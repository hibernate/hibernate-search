/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A tracker for paths actually affected by an indexed embedded definition.
 * <p>
 * Used to detect invalid configuration in an indexed embedded definition,
 * for example useless includePaths.
 */
public final class IndexedEmbeddedPathTracker {

	private final IndexedEmbeddedDefinition definition;

	/**
	 * The {@code paths} that were encountered.
	 */
	// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
	private final Map<String, Boolean> encounteredFieldPaths = new LinkedHashMap<>();

	private final Map<String, Boolean> childEncounteredFieldPaths = new LinkedHashMap<>();

	public IndexedEmbeddedPathTracker(IndexedEmbeddedDefinition definition) {
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
				// since it's exclude we should always get a FALSE value from this map for such key.
				// Hence, we only want to check for fields that we never encountered i.e. some typos in property names or just some "pointless" text :)
					!encounteredFieldPaths.containsKey( path )
							// OR the field was also "explicitly" excluded by a child, which would be redundant...
							// If it wasn't encountered then it is already covered by a containsKey check above.
							|| !childEncounteredFieldPaths.getOrDefault( path, Boolean.TRUE )
			) {
				// An "excludePaths" filter that does not result in exclusion is useless
				uselessExcludePaths.add( path );
			}
		}
		return uselessExcludePaths;
	}

	public Set<String> encounteredFieldPaths() {
		return encounteredFieldPaths.keySet();
	}

	public void markAsEncountered(String relativePath, boolean includedByThis, boolean includedByChild) {
		encounteredFieldPaths.merge(
				relativePath, includedByThis,
				(included1, included2) -> included1 || included2
		);
		childEncounteredFieldPaths.merge(
				relativePath, includedByChild,
				(included1, included2) -> included1 || included2
		);
	}
}
