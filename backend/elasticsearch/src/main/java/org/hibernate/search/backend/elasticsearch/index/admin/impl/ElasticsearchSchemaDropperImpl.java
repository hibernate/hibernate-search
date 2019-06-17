/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

/**
 * The default {@link ElasticsearchSchemaDropper} implementation.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaDropperImpl implements ElasticsearchSchemaDropper {

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public ElasticsearchSchemaDropperImpl(ElasticsearchSchemaAccessor schemaAccessor) {
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public void dropIfExisting(URLEncodedString indexName) {
		// Not actually needed, but do it to avoid cluttering the ES log
		if ( ! schemaAccessor.indexExists( indexName ) ) {
			return;
		}

		schemaAccessor.dropIndexIfExisting( indexName );
	}

}
