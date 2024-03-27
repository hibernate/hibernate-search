/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class IndexNamesRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, IndexNames> indexNamesByName = new ConcurrentHashMap<>();

	void register(IndexNames newIndexNames) {
		// Put everything in a set to avoid failures if, for example, the Hibernate Search name is identical to the read name
		// (that's not a problem per se, and should be checked elsewhere).
		Set<String> names = new LinkedHashSet<>();
		names.add( newIndexNames.write().original );
		names.add( newIndexNames.read().original );
		// Also prevent other indexes from using the Hibernate Search index name as their name/alias.
		// This is just to avoid confusing setups.
		names.add( IndexNames.normalizeName( newIndexNames.hibernateSearchIndex() ) );

		for ( String name : names ) {
			IndexNames existingIndexNames = indexNamesByName.putIfAbsent( name, newIndexNames );
			if ( existingIndexNames != null ) {
				throw log.conflictingIndexNames(
						existingIndexNames.hibernateSearchIndex(),
						newIndexNames.hibernateSearchIndex(),
						name
				);
			}
		}
	}
}
