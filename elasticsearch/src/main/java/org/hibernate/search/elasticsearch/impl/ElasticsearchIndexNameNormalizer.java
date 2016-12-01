/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.Locale;

/**
 * Applies rules imposed by Elasticsearch to index names.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchIndexNameNormalizer {

	private ElasticsearchIndexNameNormalizer() {
	}

	public static String getElasticsearchIndexName(String indexName) {
		String esIndexName = indexName.toLowerCase( Locale.ENGLISH );
		return esIndexName;
	}
}
