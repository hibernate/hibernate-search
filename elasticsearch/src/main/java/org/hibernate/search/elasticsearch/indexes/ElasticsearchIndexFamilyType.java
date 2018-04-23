/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.indexes;

import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.indexes.IndexFamilyType;

public final class ElasticsearchIndexFamilyType {

	private ElasticsearchIndexFamilyType() {
	}

	/**
	 * @return An {@link IndexFamilyType} representing the family of indexes using the Elasticsearch technology.
	 */
	public static IndexFamilyType get() {
		return ElasticsearchIndexManagerType.INSTANCE;
	}

}
