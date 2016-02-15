/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Locale;

/**
 * Applies rules imposed by Elasticsearch to index names.
 *
 * @author Gunnar Morling
 */
public class IndexNameNormalizer {

	private IndexNameNormalizer() {
	}

	public static String getElasticsearchIndexName(String indexName) {
		String esIndexName = indexName.toLowerCase( Locale.ENGLISH );
		if ( !esIndexName.equals( indexName ) ) {
			// TODO LOG
			// TODO: if index lowercasing introduces a possible ambiguity in the ES case, maybe we should validate for this
			// at the root of all IndexManagers during bootstrap?
		}

		return esIndexName;
	}
}
