/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;

/**
 * A tracker for paths actually affected by an indexed embedded definition.
 * <p>
 * Used to detect invalid configuration in an indexed embedded definition,
 * for example useless includePaths.
 */
class IndexedEmbeddedPathTracker {

	private final IndexedEmbeddedDefinition definition;

	/**
	 * The {@code paths} that were encountered, i.e. passed to {@link IndexSchemaFilter#isPathIncluded(String)}
	 * or to the same method of a child filter
	 */
	// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
	private final Map<String, Boolean> encounteredFieldPaths = new LinkedHashMap<>();

	IndexedEmbeddedPathTracker(IndexedEmbeddedDefinition definition) {
		this.definition = definition;
	}

	Set<String> getUselessIncludePaths() {
		Set<String> uselessIncludePaths = new LinkedHashSet<>();
		for ( String path : definition.getIncludePaths() ) {
			Boolean included = encounteredFieldPaths.get( path );
			if ( included == null /* not encountered */ || !included ) {
				// An "includePaths" filter that does not result in inclusion is useless
				uselessIncludePaths.add( path );
			}
		}
		return uselessIncludePaths;
	}

	Set<String> getEncounteredFieldPaths() {
		return encounteredFieldPaths.keySet();
	}

	void markAsEncountered(String relativePath, boolean included) {
		encounteredFieldPaths.merge(
				relativePath, included,
				(included1, included2) -> included1 || included2
		);
	}
}
