/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class IndexNamesRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, String> hibernateSearchIndexNameByElasticseachIndexName = new ConcurrentHashMap<>();

	void register(String elasticsearchIndexName, String hibernateSearchIndexName) {
		String existingHibernateSearchIndexName =
				hibernateSearchIndexNameByElasticseachIndexName.putIfAbsent( elasticsearchIndexName, hibernateSearchIndexName );
		if ( existingHibernateSearchIndexName != null ) {
			throw log.duplicateNormalizedIndexNames(
					existingHibernateSearchIndexName,
					hibernateSearchIndexName,
					elasticsearchIndexName
			);
		}
	}
}
