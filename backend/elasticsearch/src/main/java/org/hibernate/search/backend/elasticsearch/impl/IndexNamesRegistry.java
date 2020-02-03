/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		names.add( newIndexNames.getWrite().original );
		names.add( newIndexNames.getRead().original );
		// Also prevent other indexes from using the Hibernate Search index name as their name/alias.
		// This is just to avoid confusing setups.
		names.add( IndexNames.normalizeName( newIndexNames.getHibernateSearch() ) );

		for ( String name : names ) {
			IndexNames existingIndexNames = indexNamesByName.putIfAbsent( name, newIndexNames );
			if ( existingIndexNames != null ) {
				throw log.conflictingIndexNames(
						existingIndexNames.getHibernateSearch(),
						newIndexNames.getHibernateSearch(),
						name
				);
			}
		}
	}
}
